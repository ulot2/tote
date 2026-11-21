package app.tote.core.model

/** What kind of value an entry holds. Drives keyboard type, validation and fill formatting. */
enum class EntryType {
    LINK,
    EMAIL,
    PHONE,
    SHORT_TEXT,
    LONG_TEXT,
}

/** Fixed v1 categories. Order here is the order sections appear in the vault. */
enum class Category {
    LINKS,
    CONTACT,
    DOCUMENTS,
    ANSWERS,
}
