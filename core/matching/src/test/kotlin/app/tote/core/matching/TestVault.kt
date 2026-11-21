package app.tote.core.matching

import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType

/** A realistic job-hunting vault, shaped like what the editor produces. Values are dummies. */
object TestVault {
    private var nextId = 1L
    private fun e(
        label: String,
        value: String,
        type: EntryType,
        category: Category,
        icon: EntryIcon,
        keywords: List<String> = emptyList(),
        profiles: Set<Long> = emptySet(),
        pinned: Boolean = false,
        uses: Int = 0,
    ) = Entry(nextId++, label, value, type, category, icon, keywords = keywords, profileIds = profiles, pinned = pinned, useCount = uses)

    val linkedin = e("LinkedIn", "linkedin.com/in/someone", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN, pinned = true, uses = 9)
    val portfolio = e("Portfolio", "someone.dev", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE, keywords = listOf("see your work"), pinned = true, uses = 4)
    val github = e("GitHub", "github.com/someone", EntryType.LINK, Category.LINKS, EntryIcon.GITHUB, uses = 5)
    val x = e("X", "x.com/someone", EntryType.LINK, Category.LINKS, EntryIcon.X)
    val email = e("Email", "someone@example.com", EntryType.EMAIL, Category.CONTACT, EntryIcon.MAIL, uses = 7)
    val phone = e("Phone", "+234 801 234 5678", EntryType.PHONE, Category.CONTACT, EntryIcon.PHONE, uses = 3)
    val location = e("Location", "Lagos, Nigeria", EntryType.SHORT_TEXT, Category.CONTACT, EntryIcon.LOCATION)
    val cvTech = e("CV (Tech)", "drive.google.com/file/d/tech", EntryType.LINK, Category.DOCUMENTS, EntryIcon.GOOGLE_DRIVE, profiles = setOf(1), uses = 2)
    val cvNonTech = e("CV (Non-tech)", "drive.google.com/file/d/nontech", EntryType.LINK, Category.DOCUMENTS, EntryIcon.GOOGLE_DRIVE, profiles = setOf(2))
    val coverLetter = e("Cover letter", "drive.google.com/file/d/cover", EntryType.LINK, Category.DOCUMENTS, EntryIcon.DOC)
    val yoe = e("Years of experience", "5", EntryType.SHORT_TEXT, Category.ANSWERS, EntryIcon.TEXT)
    val bio = e("Short bio", "I build calm, useful Android apps.", EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT)
    val why = e("Why this company?", "I admire how your team ships.", EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT)

    val all = listOf(linkedin, portfolio, github, x, email, phone, location, cvTech, cvNonTech, coverLetter, yoe, bio, why)
}
