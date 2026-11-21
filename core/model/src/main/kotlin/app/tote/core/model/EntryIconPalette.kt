package app.tote.core.model

/**
 * Badge colours for entry icons as ARGB, shared by the app's Compose badges and the autofill
 * dropdown (which draws bitmaps), so both look the same.
 */
object EntryIconPalette {

    data class Colors(val container: Long, val content: Long)

    private val monoDark = Colors(0xFFF5F2EA, 0xFF111111)
    private val monoLight = Colors(0xFF16150F, 0xFFFFFFFF)
    private val neutralDark = Colors(0xFF2C2B26, 0xFFF5F2EA)
    private val neutralLight = Colors(0xFFE5E0D4, 0xFF1B1A16)

    /** Brand colours, or null for the neutral (theme-surface) icons. */
    fun brand(icon: EntryIcon, dark: Boolean): Colors? = when (icon) {
        EntryIcon.LINKEDIN -> Colors(0xFF0A66C2, 0xFFFFFFFF)
        // Monochrome brands get an inverted tile so they never vanish into the background.
        EntryIcon.GITHUB, EntryIcon.X, EntryIcon.MEDIUM -> if (dark) monoDark else monoLight
        EntryIcon.BEHANCE -> Colors(0xFF1769FF, 0xFFFFFFFF)
        EntryIcon.DRIBBBLE -> Colors(0xFFEA4C89, 0xFFFFFFFF)
        EntryIcon.GOOGLE_DRIVE -> Colors(0xFF1A73E8, 0xFFFFFFFF)
        else -> null
    }

    /** Colours for any icon, using Tote's default neutral surface for non-brand icons. */
    fun colors(icon: EntryIcon, dark: Boolean): Colors = brand(icon, dark) ?: if (dark) neutralDark else neutralLight

    /** Brands drawn as letters because their mark doesn't read at badge size (or isn't available). */
    fun monogram(icon: EntryIcon): String? = when (icon) {
        EntryIcon.LINKEDIN -> "in"
        EntryIcon.MEDIUM -> "M"
        else -> null
    }
}
