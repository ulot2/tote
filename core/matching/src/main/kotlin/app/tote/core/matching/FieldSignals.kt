package app.tote.core.matching

/** Coarse input type of a field, from Android input type flags or the HTML `type`. */
enum class InputKind { TEXT, EMAIL, PHONE, URI, NUMBER, PASSWORD, DATE, HIDDEN }

/**
 * Everything the autofill service could learn about one form field (Plan §7.1).
 * All strings are raw; the matcher normalises them. Leave unknown signals null.
 */
data class FieldSignals(
    /** Android `autofillHints`, e.g. "emailAddress". */
    val autofillHints: List<String> = emptyList(),
    /** HTML `autocomplete` attribute, e.g. "email" or "section-a tel". */
    val htmlAutocomplete: String? = null,
    /** HTML `type`, e.g. "url", "email", "textarea". */
    val htmlType: String? = null,
    val htmlName: String? = null,
    val htmlId: String? = null,
    val placeholder: String? = null,
    val ariaLabel: String? = null,
    /** `<label>` text for web fields. */
    val label: String? = null,
    /** Native view hint text. */
    val hintText: String? = null,
    /** Native view id resource entry name, e.g. "et_linkedin". */
    val resourceId: String? = null,
    val contentDescription: String? = null,
    /** Closest preceding text in the same container (Plan §7.1 item 4). */
    val nearbyText: String? = null,
    val inputKind: InputKind = InputKind.TEXT,
    val multiline: Boolean = false,
    /** False when the app marked the view as not important for autofill. */
    val importantForAutofill: Boolean = true,
    val webDomain: String? = null,
    val packageName: String? = null,
) {
    /** Where the field lives, used as the learning key's first half (Plan §6.3). */
    val source: String? get() = webDomain?.lowercase()?.removePrefix("www.") ?: packageName

    /** Effective input kind, combining the Android flags with the HTML type. */
    val effectiveKind: InputKind
        get() = when (htmlType?.lowercase()) {
            "email" -> InputKind.EMAIL
            "tel" -> InputKind.PHONE
            "url" -> InputKind.URI
            "number" -> InputKind.NUMBER
            "password" -> InputKind.PASSWORD
            "date", "datetime-local", "month", "week", "time" -> InputKind.DATE
            "hidden" -> InputKind.HIDDEN
            else -> inputKind
        }

    val isMultiline: Boolean get() = multiline || htmlType.equals("textarea", ignoreCase = true)
}
