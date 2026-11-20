package app.tote.feature.vault

import app.tote.core.model.Category
import app.tote.core.model.EntryType

fun Category.title(): String = when (this) {
    Category.LINKS -> "Links"
    Category.CONTACT -> "Contact"
    Category.DOCUMENTS -> "Documents"
    Category.ANSWERS -> "Answers"
}

fun EntryType.title(): String = when (this) {
    EntryType.LINK -> "Link"
    EntryType.EMAIL -> "Email"
    EntryType.PHONE -> "Phone"
    EntryType.SHORT_TEXT -> "Short text"
    EntryType.LONG_TEXT -> "Long text"
}
