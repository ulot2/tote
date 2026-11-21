package app.tote.core.matching

/** A normalised signal: stemmed tokens plus a compact (space-free) form. */
class SignalText(raw: String) {
    val tokens: List<String> = TextNormalizer.tokens(raw).map(TextNormalizer::stem)
    val compact: String = tokens.joinToString("")
    val isEmpty: Boolean get() = tokens.isEmpty()
}

/**
 * Where a phrase was found in a [SignalText], as a token range. [isCompact] hits were found inside a
 * single unsplit token ("linkedinprofileurl", or "website" for "web site"); their range is that
 * token, so a longer phrase around it can still swallow them.
 */
data class PhraseHit(val phrase: Phrase, val range: IntRange?, val isCompact: Boolean = false)

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
        if (phrase.compact.length >= MIN_COMPACT_LENGTH) {
            val i = text.tokens.indexOfFirst { it.contains(phrase.compact) }
            if (i >= 0) return PhraseHit(phrase, i..i, isCompact = true)
        }
        return null
    }

    /**
     * A hit is swallowed when a longer phrase in [claimants] covers a strictly larger span around it.
     * That stops "address" (location, weak) firing inside "email address", or "contact" inside
     * "contact email".
     */
    fun isSwallowed(text: SignalText, hit: PhraseHit, claimants: List<Phrase>): Boolean {
        val range = hit.range ?: return false
        // Compare how much of the text each covers, not phrase length: a compact "web site" hit
        // covers one token ("website") and "company website" covers two.
        val width = range.last - range.first
        return claimants.any { other ->
            val o = find(text, other)?.range
            o != null && o.last - o.first > width && o.first <= range.first && o.last >= range.last
        }
    }
}
