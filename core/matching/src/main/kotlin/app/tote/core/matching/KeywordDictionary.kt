package app.tote.core.matching

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What a well-known entry is about. Inferred from an entry's icon, type and label (see [EntryKinds]). */
enum class EntryKind(val key: String) {
    LINKEDIN("linkedin"),
    GITHUB("github"),
    PORTFOLIO("portfolio"),
    TWITTER("twitter"),
    BEHANCE("behance"),
    DRIBBBLE("dribbble"),
    BLOG("blog"),
    EMAIL("email"),
    PHONE("phone"),
    CV("cv"),
    COVER_LETTER("cover_letter"),
    LOCATION("location"),
    BIO("bio"),
    YEARS_EXPERIENCE("years_experience"),
    WHY_COMPANY("why_company"),
    SALARY("salary"),
    NOTICE_PERIOD("notice_period"),
    ;

    companion object {
        fun fromKey(key: String): EntryKind? = entries.firstOrNull { it.key == key }
    }
}

enum class IgnoreReason { NOT_IMPORTANT, PASSWORD, OTP, CARD, SEARCH, CAPTCHA, HIDDEN }

/** A phrase, pre-split into stemmed tokens. */
data class Phrase(val text: String, val tokens: List<String>) {
    /** Tokens joined without spaces, for ids like "linkedinurl" that have no separators. */
    val compact: String = tokens.joinToString("")

    companion object {
        fun of(raw: String): Phrase {
            val tokens = TextNormalizer.tokens(raw).map(TextNormalizer::stem)
            return Phrase(tokens.joinToString(" "), tokens)
        }
    }
}

/** What a hint or autocomplete token tells us. */
sealed interface HintMeaning {
    data class Kind(val kind: EntryKind) : HintMeaning
    data object Link : HintMeaning
    data class Ignore(val reason: IgnoreReason) : HintMeaning
}

/**
 * The keyword dictionary (Plan §7.3), loaded from `dictionary.json`, a format shared with the
 * planned TypeScript port (Plan §14).
 */
class KeywordDictionary(
    val strong: Map<EntryKind, List<Phrase>>,
    val weak: Map<EntryKind, List<Phrase>>,
    val genericLink: List<Phrase>,
    val ignore: Map<IgnoreReason, List<Phrase>>,
    private val androidHints: Map<String, HintMeaning>,
    private val htmlAutocomplete: Map<String, HintMeaning>,
) {
    /**
     * Every kind phrase, used to decide which shorter hits a longer phrase swallows. Generic link
     * words are left out on purpose: "other website" must not hide the portfolio's "website".
     */
    val kindPhrases: List<Phrase> = (strong.values.flatten() + weak.values.flatten()).distinct()

    fun androidHint(hint: String): HintMeaning? = androidHints[hint.lowercase().replace(Regex("[^a-z0-9]"), "")]

    /** Reads the meaningful token of an autocomplete value like "section-x shipping tel". */
    fun autocomplete(value: String): HintMeaning? =
        value.lowercase().split(' ').filter { it.isNotBlank() }.asReversed().firstNotNullOfOrNull { htmlAutocomplete[it] }

    @Serializable
    private data class KindJson(val strong: List<String> = emptyList(), val weak: List<String> = emptyList())

    @Serializable
    private data class DictionaryJson(
        val version: Int,
        val kinds: Map<String, KindJson>,
        val genericLink: List<String>,
        val ignore: Map<String, List<String>>,
        val androidHints: Map<String, String>,
        val htmlAutocomplete: Map<String, String>,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** The bundled dictionary. Parsed once, lazily (a few ms), then shared. */
        val default: KeywordDictionary by lazy {
            val text = KeywordDictionary::class.java.getResourceAsStream("dictionary.json")
                ?.bufferedReader()?.use { it.readText() }
                ?: error("dictionary.json missing from :core:matching resources")
            parse(text)
        }

        fun parse(text: String): KeywordDictionary {
            val d = json.decodeFromString<DictionaryJson>(text)
            fun kind(key: String) = EntryKind.fromKey(key) ?: error("Unknown kind '$key' in dictionary")
            fun meaning(v: String): HintMeaning = when {
                v == "@link" -> HintMeaning.Link
                v.startsWith("@ignore:") -> HintMeaning.Ignore(IgnoreReason.valueOf(v.removePrefix("@ignore:").uppercase()))
                else -> HintMeaning.Kind(kind(v))
            }
            return KeywordDictionary(
                strong = d.kinds.entries.associate { (k, v) -> kind(k) to v.strong.map(Phrase::of) },
                weak = d.kinds.entries.associate { (k, v) -> kind(k) to v.weak.map(Phrase::of) },
                genericLink = d.genericLink.map(Phrase::of),
                ignore = d.ignore.entries.associate { (k, v) -> IgnoreReason.valueOf(k.uppercase()) to v.map(Phrase::of) },
                androidHints = d.androidHints.mapValues { meaning(it.value) },
                htmlAutocomplete = d.htmlAutocomplete.mapValues { meaning(it.value) },
            )
        }
    }
}
