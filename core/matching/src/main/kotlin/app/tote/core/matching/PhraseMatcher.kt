package app.tote.core.matching

/** A normalised signal: stemmed tokens plus a compact (space-free) form. */
class SignalText(raw: String) {
    val tokens: List<String> = TextNormalizer.tokens(raw).map(TextNormalizer::stem)
    val compact: String = tokens.joinToString("")
    val isEmpty: Boolean get() = tokens.isEmpty()
}

/** Where a phrase was found in a [SignalText]. A null [range] means a compact (substring) match. */
data class PhraseHit(val phrase: Phrase, val range: IntRange?) {
    val isCompact: Boolean get() = range == null
}

object PhraseMatcher {

    /** Compact matching only for distinctive phrases: "linkedinurl" yes, "cv" inside "cvv" no. */
    private const val MIN_COMPACT_LENGTH = 6

    /** Finds [phrase] in [text] at token boundaries, or failing that inside a single unsplit id. */
    fun find(text: SignalText, phrase: Phrase): PhraseHit? {
        val n = phrase.tokens.size
        if (n == 0 || text.isEmpty) return null
        for (i in 0..text.tokens.size - n) {
            if (text.tokens.subList(i, i + n) == phrase.tokens) return PhraseHit(phrase, i until i + n)
        }
        if (phrase.compact.length >= MIN_COMPACT_LENGTH && text.compact.contains(phrase.compact)) return PhraseHit(phrase, null)
        return null
    }

    /**
     * A hit is swallowed when a longer phrase in [claimants] covers a strictly larger span around it.
     * That stops "address" (location, weak) firing inside "email address", or "contact" inside
     * "contact email".
     */
    fun isSwallowed(text: SignalText, hit: PhraseHit, claimants: List<Phrase>): Boolean {
        val range = hit.range ?: return false
        return claimants.any { other ->
            other.tokens.size > hit.phrase.tokens.size && run {
                val o = find(text, other)?.range
                o != null && o.first <= range.first && o.last >= range.last
            }
        }
    }
}
