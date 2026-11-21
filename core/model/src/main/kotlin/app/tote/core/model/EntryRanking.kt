package app.tote.core.model

/** Ordering and search shared by the picker sheet (tile now, autofill "All entries…" later). */
object EntryRanking {

    /**
     * Picker order (Plan §8.4): pinned first, then most used, then most recently used,
     * then the vault's own order (category, manual sort).
     */
    fun forPicker(entries: List<Entry>): List<Entry> = entries.sortedWith(
        compareByDescending<Entry> { it.pinned }
            .thenByDescending { it.useCount }
            .thenByDescending { it.lastUsedAt ?: 0L }
            .thenBy { it.category.ordinal }
            .thenBy { it.sortOrder },
    )

    /** Case-insensitive match on label, value and keywords. Blank query matches everything. */
    fun matches(entry: Entry, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return entry.label.contains(q, ignoreCase = true) ||
            entry.value.contains(q, ignoreCase = true) ||
            entry.keywords.any { it.contains(q, ignoreCase = true) }
    }

    /** Search results keep picker order, but entries whose label starts with the query come first. */
    fun search(entries: List<Entry>, query: String): List<Entry> {
        val q = query.trim()
        val ranked = forPicker(entries)
        if (q.isEmpty()) return ranked
        val (prefix, rest) = ranked.filter { matches(it, q) }.partition { it.label.startsWith(q, ignoreCase = true) }
        return prefix + rest
    }
}
