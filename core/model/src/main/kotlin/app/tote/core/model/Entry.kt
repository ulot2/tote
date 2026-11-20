package app.tote.core.model

/**
 * A single reusable value in the vault.
 *
 * Timestamps are epoch milliseconds. [profileIds] empty means the entry belongs to "All" profiles.
 */
data class Entry(
    val id: Long = 0,
    val label: String,
    val value: String,
    val type: EntryType,
    val category: Category,
    val icon: EntryIcon,
    /** True when the user picked [icon] by hand, so auto-detection must not replace it. */
    val iconOverridden: Boolean = false,
    val keywords: List<String> = emptyList(),
    val profileIds: Set<Long> = emptySet(),
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val isInAllProfiles: Boolean get() = profileIds.isEmpty()
}

/** A named set of entries, e.g. "Tech" or "Non-tech". Exactly one is active at a time. */
data class Profile(
    val id: Long = 0,
    val name: String,
    /** Optional accent colour as ARGB. */
    val accentArgb: Long? = null,
    val sortOrder: Int = 0,
)
