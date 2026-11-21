package app.tote.core.model

/** One-tap starting points shown in onboarding and in the empty vault (Plan §6.4). */
data class SeedTemplate(
    val key: String,
    val label: String,
    val type: EntryType,
    val category: Category,
    val icon: EntryIcon,
    /** Example shown as the value field's placeholder. Never stored. */
    val placeholder: String,
)

object SeedTemplates {
    val all = listOf(
        SeedTemplate("linkedin", "LinkedIn", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN, "linkedin.com/in/your-name"),
        SeedTemplate("portfolio", "Portfolio", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE, "yourname.com"),
        SeedTemplate("github", "GitHub", EntryType.LINK, Category.LINKS, EntryIcon.GITHUB, "github.com/your-handle"),
        SeedTemplate("email", "Email", EntryType.EMAIL, Category.CONTACT, EntryIcon.MAIL, "you@example.com"),
        SeedTemplate("phone", "Phone", EntryType.PHONE, Category.CONTACT, EntryIcon.PHONE, "+1 555 010 0000"),
        SeedTemplate("cv", "CV link", EntryType.LINK, Category.DOCUMENTS, EntryIcon.DOC, "drive.google.com/…"),
        SeedTemplate("location", "Location", EntryType.SHORT_TEXT, Category.CONTACT, EntryIcon.LOCATION, "City, Country"),
        SeedTemplate("yoe", "Years of experience", EntryType.SHORT_TEXT, Category.ANSWERS, EntryIcon.TEXT, "5"),
        SeedTemplate("bio", "Short bio", EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT, "A few sentences about you…"),
        SeedTemplate("why", "Why this company?", EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT, "A reusable draft you tweak per application…"),
    )

    fun byKey(key: String?): SeedTemplate? = all.firstOrNull { it.key == key }
}
