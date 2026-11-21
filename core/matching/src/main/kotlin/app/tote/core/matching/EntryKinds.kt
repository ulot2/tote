package app.tote.core.matching

import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType

/** Infers which well-known [EntryKind] an entry is, from its icon, type and label (Plan §7.3). */
object EntryKinds {

    fun of(entry: Entry, dictionary: KeywordDictionary = KeywordDictionary.default): EntryKind? {
        when (entry.icon) {
            EntryIcon.LINKEDIN -> return EntryKind.LINKEDIN
            EntryIcon.GITHUB -> return EntryKind.GITHUB
            EntryIcon.X -> return EntryKind.TWITTER
            EntryIcon.BEHANCE -> return EntryKind.BEHANCE
            EntryIcon.DRIBBBLE -> return EntryKind.DRIBBBLE
            EntryIcon.MEDIUM -> return EntryKind.BLOG
            else -> Unit
        }
        when (entry.type) {
            EntryType.EMAIL -> return EntryKind.EMAIL
            EntryType.PHONE -> return EntryKind.PHONE
            else -> Unit
        }
        if (entry.icon == EntryIcon.LOCATION) return EntryKind.LOCATION

        // Otherwise read the label: the earliest, longest dictionary phrase wins
        // ("Why this company?" → why_company, "CV (Non-tech)" → cv, "Personal website" → portfolio).
        val label = SignalText(entry.label)
        val fromLabel = dictionary.strong.entries
            .flatMap { (kind, phrases) -> phrases.mapNotNull { p -> PhraseMatcher.find(label, p)?.let { kind to it } } }
            .minWithOrNull(compareBy<Pair<EntryKind, PhraseHit>> { it.second.range?.first ?: Int.MAX_VALUE }.thenByDescending { it.second.phrase.tokens.size })
            ?.first
        if (fromLabel != null) return fromLabel

        return when {
            entry.icon == EntryIcon.GOOGLE_DRIVE || entry.icon == EntryIcon.DOC -> EntryKind.CV
            entry.type == EntryType.LINK && entry.icon == EntryIcon.GLOBE -> EntryKind.PORTFOLIO
            else -> null
        }
    }
}
