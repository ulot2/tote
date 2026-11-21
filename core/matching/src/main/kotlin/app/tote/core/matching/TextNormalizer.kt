package app.tote.core.matching

import java.text.Normalizer

/**
 * Normalises raw field signals (hints, ids, labels) into comparable tokens (Plan §7.2):
 * lowercase, camelCase and snake_case split, accents folded, punctuation stripped,
 * whitespace collapsed. Filler words are kept; they simply never match a dictionary phrase.
 */
object TextNormalizer {

    private val camelBoundary = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
    private val combiningMarks = Regex("\\p{M}+")

    /** "linkedinProfile_URL" → "linkedin profile url"; "Résumé/CV" → "resume cv". */
    fun normalize(raw: String): String =
        Normalizer.normalize(raw.replace(camelBoundary, " "), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase()
            .replace(nonAlphanumeric, " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun tokens(raw: String): List<String> = normalize(raw).split(' ').filter { it.isNotEmpty() }

    /**
     * Light stemming so "links"/"link" and "urls"/"url" compare equal. Deliberately crude:
     * only a trailing "s", and never on short words or "-ss"/"-us" endings ("address", "status").
     */
    fun stem(token: String): String =
        if (token.length > 3 && token.endsWith('s') && !token.endsWith("ss") && !token.endsWith("us")) token.dropLast(1) else token
}
