package app.tote.ui

import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.feature.vault.VaultSection
import app.tote.feature.vault.VaultUiState

/** Manual test data (Plan §6.4) used only by tests. */
object SampleData {
    val linkedin = Entry(1, "LinkedIn", "linkedin.com/in/toluwalope-adegoke", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN, pinned = true)
    val portfolio = Entry(2, "Portfolio", "https://www.tnuell.site", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE, pinned = true)
    val github = Entry(3, "GitHub", "github.com/ulot2", EntryType.LINK, Category.LINKS, EntryIcon.GITHUB)
    val email = Entry(4, "Email", "tolu.nuell@gmail.com", EntryType.EMAIL, Category.CONTACT, EntryIcon.MAIL, pinned = true)
    val phone = Entry(5, "Phone", "+234 801 234 5678", EntryType.PHONE, Category.CONTACT, EntryIcon.PHONE)
    val cv = Entry(6, "CV (Tech)", "https://drive.google.com/file/d/1abc/view", EntryType.LINK, Category.DOCUMENTS, EntryIcon.GOOGLE_DRIVE)
    val bio = Entry(
        7, "Short bio",
        "Android developer who loves calm, well-crafted tools. Five years shipping Kotlin apps used by thousands.",
        EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT,
    )

    val all = listOf(linkedin, portfolio, github, email, phone, cv, bio)

    fun state(entries: List<Entry> = all, query: String = "", reordering: Boolean = false) = VaultUiState(
        loading = false,
        totalCount = entries.size,
        query = query,
        pinned = if (query.isBlank() && !reordering) entries.filter { it.pinned } else emptyList(),
        sections = Category.entries.mapNotNull { c ->
            entries.filter { it.category == c && (query.isBlank() || it.label.contains(query, true)) }
                .takeIf { it.isNotEmpty() }?.let { VaultSection(c, it) }
        },
        reordering = reordering,
    )
}
