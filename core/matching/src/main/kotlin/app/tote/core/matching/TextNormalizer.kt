package app.tote.core.matching

/**
 * Normalises raw field signals (hints, ids, labels) into comparable tokens (Plan §7.2).
 *
 * Pure Kotlin: this module must never depend on Android so it stays testable on the JVM.
 * The full matching engine (dictionary, scoring, ignore rules) lands in Phase 3.
 */
object TextNormalizer {

    private val camelBoundary = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")

    /** "linkedinProfile_URL" → "linkedin profile url". */
    fun normalize(raw: String): String =
        raw.replace(camelBoundary, " ")
            .lowercase()
            .replace(nonAlphanumeric, " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun tokens(raw: String): List<String> = normalize(raw).split(' ').filter { it.isNotEmpty() }
}
