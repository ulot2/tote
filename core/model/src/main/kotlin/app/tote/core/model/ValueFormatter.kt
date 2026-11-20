package app.tote.core.model

/** Display and fill formatting for entry values (Plan §6.5). */
object ValueFormatter {

    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://")

    /** How a value is shown in the UI. Links drop their scheme and a trailing slash. */
    fun display(type: EntryType, value: String): String = when (type) {
        EntryType.LINK -> value.trim().replaceFirst(schemeRegex, "").removeSuffix("/")
        EntryType.LONG_TEXT -> value.trim().replace(Regex("\\s+"), " ")
        else -> value.trim()
    }

    /**
     * The value to insert into a field. Links get `https://` when the target is a URL field and
     * the stored value has no scheme. Everything else is inserted exactly as stored.
     */
    fun forFill(type: EntryType, value: String, targetIsUrlField: Boolean): String =
        if (type == EntryType.LINK && targetIsUrlField) withScheme(value) else value

    /** The value copied to the clipboard. Stored as entered, so copy it as entered. */
    fun forClipboard(value: String): String = value

    fun withScheme(value: String): String {
        val v = value.trim()
        return if (schemeRegex.containsMatchIn(v)) v else "https://$v"
    }
}
