package app.tote.core.model

/**
 * Icon shown for an entry. Brand icons are auto-detected from a link's host (see [EntryClassifier]);
 * the user can override the choice in the editor.
 */
enum class EntryIcon(val isBrand: Boolean) {
    LINKEDIN(true),
    GITHUB(true),
    X(true),
    BEHANCE(true),
    DRIBBBLE(true),
    MEDIUM(true),
    GOOGLE_DRIVE(true),
    GLOBE(false),
    MAIL(false),
    PHONE(false),
    DOC(false),
    LOCATION(false),
    TEXT(false),
    ;

    companion object {
        /** The neutral icon for a type, used when no brand is detected. */
        fun defaultFor(type: EntryType): EntryIcon = when (type) {
            EntryType.LINK -> GLOBE
            EntryType.EMAIL -> MAIL
            EntryType.PHONE -> PHONE
            EntryType.SHORT_TEXT, EntryType.LONG_TEXT -> TEXT
        }
    }
}
