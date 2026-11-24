package app.tote.core.matching

/**
 * Key for local learning (Plan §6.3): the field's source plus a normalised signature built from its
 * most stable signals. Two visits to the same form field produce the same key.
 */
data class LearningKey(val source: String, val signature: String)

object FieldSignature {

    fun of(field: FieldSignals): String {
        val parts = listOfNotNull(
            field.htmlAutocomplete?.let { "ac:" + TextNormalizer.normalize(it) },
            field.autofillHints.takeIf { it.isNotEmpty() }?.let { "hint:" + it.joinToString(",") { h -> TextNormalizer.normalize(h) } },
            (field.htmlId ?: field.resourceId)?.let { "id:" + TextNormalizer.normalize(it) },
            field.htmlName?.let { "name:" + TextNormalizer.normalize(it) },
            (field.label ?: field.ariaLabel ?: field.placeholder ?: field.hintText)?.let { "label:" + TextNormalizer.normalize(it).take(80) },
        )
        return parts.joinToString("|")
    }

    /**
     * A short human description of a stored signature for the "Learned suggestions" screen:
     * the field's label if known, else its name or id. ("label:linked in profile" → "linked in profile")
     */
    fun describe(signature: String): String {
        val parts = signature.split('|').associate { it.substringBefore(':') to it.substringAfter(':', "") }
        return listOf("label", "name", "id", "ac", "hint").firstNotNullOfOrNull { parts[it]?.takeIf(String::isNotBlank) } ?: "a field"
    }

    fun key(field: FieldSignals): LearningKey? {
        val source = field.source ?: return null
        val signature = of(field).ifEmpty { return null }
        return LearningKey(source, signature)
    }
}
