package app.tote.core.matching

import app.tote.core.model.Entry
import app.tote.core.model.EntryRanking
import app.tote.core.model.EntryType

/** Per-request context: the active profile and what Tote learned for this field. */
data class MatchContext(
    /** null = the implicit "All" profile: every entry is eligible. */
    val activeProfileId: Long? = null,
    /** Entry id → times the user picked it for this exact source + field signature (Plan §6.3). */
    val learned: Map<Long, Int> = emptyMap(),
    val maxSuggestions: Int = 3,
)

data class ScoredEntry(
    val entry: Entry,
    val score: Int,
    /** Why it scored, for tests and debug logs. Never contains entry values. */
    val reasons: List<String>,
)

data class MatchResult(
    /** Non-null when the field must get no suggestions at all (Plan §7.5). */
    val ignored: IgnoreReason?,
    val suggestions: List<ScoredEntry>,
    /** True when nothing matched and [suggestions] are just the most used entries. */
    val isFallback: Boolean,
    /** Whether to append the "All entries…" option. */
    val showAllEntries: Boolean,
) {
    val top: Entry? get() = suggestions.firstOrNull()?.entry

    companion object {
        fun ignored(reason: IgnoreReason) = MatchResult(reason, emptyList(), isFallback = false, showAllEntries = false)
    }
}

/**
 * The field-matching engine (Plan §7). Pure Kotlin, no Android.
 *
 * Scoring, per entry:
 * - an explicit hint (Android autofill hint or HTML autocomplete) for the entry's kind: [HINT]
 * - the best phrase hit in each signal tier, summed: label-like text [STRONG], name/id [MEDIUM],
 *   nearby text [NEARBY]; weak dictionary phrases score half, unsplit-id (compact) hits 80%
 * - generic link words ("url", "other link") or a URL-typed field: every link entry gets [GENERIC_LINK]
 * - type fit: URL field boosts links and sinks the rest; email/tel/number fields exclude
 *   incompatible entries; multiline fields favour long text
 * - learned mapping for this source + signature: [LEARNED] (top boost)
 * Pinned, use count and recency only break ties.
 */
class FieldMatcher(private val dictionary: KeywordDictionary = KeywordDictionary.default) {

    fun match(field: FieldSignals, entries: List<Entry>, context: MatchContext = MatchContext()): MatchResult {
        IgnoreRules.check(field, dictionary)?.let { return MatchResult.ignored(it) }

        val kind = field.effectiveKind
        val pool = entries
            .filter { e -> context.activeProfileId == null || e.profileIds.isEmpty() || context.activeProfileId in e.profileIds }
            .filter { e -> fitsInputKind(e, kind) }

        val hintKinds = mutableSetOf<EntryKind>()
        var linkHinted = kind == InputKind.URI
        for (h in field.autofillHints) when (val m = dictionary.androidHint(h)) {
            is HintMeaning.Kind -> hintKinds += m.kind
            HintMeaning.Link -> linkHinted = true
            else -> Unit
        }
        field.htmlAutocomplete?.let { ac ->
            when (val m = dictionary.autocomplete(ac)) {
                is HintMeaning.Kind -> hintKinds += m.kind
                HintMeaning.Link -> linkHinted = true
                else -> Unit
            }
        }
        if (kind == InputKind.EMAIL) hintKinds += EntryKind.EMAIL
        if (kind == InputKind.PHONE) hintKinds += EntryKind.PHONE

        val tiers = listOf(
            Tier("label", STRONG, listOfNotNull(field.label, field.placeholder, field.ariaLabel, field.hintText, field.contentDescription)),
            Tier("id", MEDIUM, listOfNotNull(field.htmlName, field.htmlId, field.resourceId)),
            Tier("nearby", NEARBY, listOfNotNull(field.nearbyText)),
        ).map { it to it.texts.map(::SignalText).filterNot(SignalText::isEmpty) }

        // Every phrase that could claim a span, so shorter hits inside longer ones are dropped.
        val entryPhrases = pool.associate { it.id to phrasesFor(it) }
        val claimants = dictionary.kindPhrases + entryPhrases.values.flatMap { list -> list.map { it.phrase } }

        val genericLink = linkHinted || tiers.any { (_, texts) ->
            texts.any { t -> dictionary.genericLink.any { p -> PhraseMatcher.find(t, p)?.let { !PhraseMatcher.isSwallowed(t, it, claimants) } == true } }
        }

        val scored = pool.map { entry ->
            val reasons = mutableListOf<String>()
            var score = 0
            val entryKind = EntryKinds.of(entry, dictionary)

            if (entryKind != null && entryKind in hintKinds) {
                score += HINT; reasons += "hint:${entryKind.key}"
            }
            var textScore = 0
            for ((tier, texts) in tiers) {
                var best = 0.0
                var bestWhy: String? = null
                for (t in texts) for (wp in entryPhrases.getValue(entry.id)) {
                    val hit = PhraseMatcher.find(t, wp.phrase) ?: continue
                    if (PhraseMatcher.isSwallowed(t, hit, claimants)) continue
                    val s = tier.weight * wp.weight * (if (hit.isCompact) COMPACT_FACTOR else 1.0)
                    if (s > best) { best = s; bestWhy = "${tier.name}:${wp.phrase.text}" }
                }
                if (best > 0) { textScore += best.toInt(); reasons += bestWhy!! }
            }
            score += textScore
            if (entry.type == EntryType.LINK) {
                if (genericLink) { score += GENERIC_LINK; reasons += "generic-link" }
                if (kind == InputKind.URI) score += URL_FIELD_BONUS
            } else if (kind == InputKind.URI) {
                score -= URL_FIELD_PENALTY
            }
            if (field.isMultiline && score > 0) {
                score += if (entry.type == EntryType.LONG_TEXT) MULTILINE_FIT else -MULTILINE_FIT
            }
            context.learned[entry.id]?.let { hits -> score += LEARNED + hits.coerceAtMost(10); reasons += "learned:$hits" }
            ScoredEntry(entry, score, reasons)
        }

        val tieBreak = compareByDescending<ScoredEntry> { it.score }
            .thenByDescending { it.entry.pinned }
            .thenByDescending { it.entry.useCount }
            .thenByDescending { it.entry.lastUsedAt ?: 0L }
            .thenBy { it.entry.sortOrder }
        val passing = scored.filter { it.score >= THRESHOLD }.sortedWith(tieBreak)
        if (passing.isNotEmpty()) {
            val top = passing.first().score
            val kept = passing.filter { it.score >= top * RELATIVE_CUTOFF }.take(context.maxSuggestions)
            return MatchResult(null, kept, isFallback = false, showAllEntries = true)
        }

        // Nothing matched. Offer the most used entries, but only where free text makes sense.
        if (!looksLikeFreeText(field)) return MatchResult(null, emptyList(), isFallback = false, showAllEntries = false)
        val fallback = EntryRanking.forPicker(pool)
            // Put the type that suits the field first: links for URL inputs, long answers for text areas.
            .sortedByDescending {
                when {
                    kind == InputKind.URI -> it.type == EntryType.LINK
                    field.isMultiline -> it.type == EntryType.LONG_TEXT
                    else -> false
                }
            }
            .take(context.maxSuggestions)
            .map { ScoredEntry(it, 0, listOf("fallback")) }
        return MatchResult(null, fallback, isFallback = true, showAllEntries = true)
    }

    private data class Tier(val name: String, val weight: Int, val texts: List<String>)
    private data class WeightedPhrase(val phrase: Phrase, val weight: Double)

    /** Dictionary phrases for the entry's kind, plus its own label and keywords. */
    private fun phrasesFor(entry: Entry): List<WeightedPhrase> {
        val kind = EntryKinds.of(entry, dictionary)
        val own = (listOf(entry.label) + entry.keywords).map(Phrase::of).filter { it.tokens.isNotEmpty() }
        return buildList {
            if (kind != null) {
                dictionary.strong[kind].orEmpty().forEach { add(WeightedPhrase(it, 1.0)) }
                dictionary.weak[kind].orEmpty().forEach { add(WeightedPhrase(it, WEAK_FACTOR)) }
            }
            own.forEach { add(WeightedPhrase(it, 1.0)) }
        }
    }

    private fun fitsInputKind(entry: Entry, kind: InputKind): Boolean = when (kind) {
        InputKind.EMAIL -> entry.type == EntryType.EMAIL
        InputKind.PHONE -> entry.type == EntryType.PHONE
        InputKind.NUMBER -> entry.value.trim().matches(Regex("[+-]?[0-9][0-9.,\\s]*"))
        // Date pickers: Tote doesn't store dates, and "2 weeks" in a date field is noise.
        InputKind.DATE -> false
        else -> true
    }

    /** Free text: plain text or URL inputs. Never number, date, email-only or phone-only inputs. */
    private fun looksLikeFreeText(field: FieldSignals): Boolean =
        field.effectiveKind == InputKind.TEXT || field.effectiveKind == InputKind.URI

    companion object {
        const val HINT = 100
        const val STRONG = 60
        const val MEDIUM = 45
        const val NEARBY = 35
        const val GENERIC_LINK = 32
        const val URL_FIELD_BONUS = 15
        const val URL_FIELD_PENALTY = 40
        const val MULTILINE_FIT = 10
        const val LEARNED = 200
        const val WEAK_FACTOR = 0.5
        const val COMPACT_FACTOR = 0.8
        const val THRESHOLD = 30
        /** Drop suggestions scoring under this share of the top one. */
        const val RELATIVE_CUTOFF = 0.3
    }
}
