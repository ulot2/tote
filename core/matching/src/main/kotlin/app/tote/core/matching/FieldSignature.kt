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

    fun key(field: FieldSignals): LearningKey? {
        val source = field.source ?: return null
        val signature = of(field).ifEmpty { return null }
        return LearningKey(source, signature)
    }
}
