package app.tote.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRankingTest {

    private fun e(
        id: Long,
        label: String,
        pinned: Boolean = false,
        uses: Int = 0,
        lastUsed: Long? = null,
        category: Category = Category.LINKS,
        order: Int = 0,
        value: String = "$label.example",
        keywords: List<String> = emptyList(),
    ) = Entry(id, label, value, EntryType.LINK, category, EntryIcon.GLOBE, keywords = keywords, pinned = pinned, sortOrder = order, useCount = uses, lastUsedAt = lastUsed)

    @Test fun pinnedThenMostUsedThenRecentThenVaultOrder() {
        val list = listOf(
            e(1, "a", order = 1),
            e(2, "b", uses = 5),
            e(3, "c", pinned = true),
            e(4, "d", uses = 5, lastUsed = 100),
            e(5, "e", order = 0, category = Category.CONTACT),
            e(6, "f", order = 0),
        )
        assertEquals(listOf(3L, 4, 2, 6, 1, 5), EntryRanking.forPicker(list).map { it.id })
    }

    @Test fun searchMatchesLabelValueAndKeywords() {
        val list = listOf(
            e(1, "GitHub", value = "github.com/me"),
            e(2, "Portfolio", value = "me.site", keywords = listOf("website")),
            e(3, "Email", value = "me@mail.com"),
        )
        assertEquals(listOf(2L), EntryRanking.search(list, "web").map { it.id })
        assertEquals(listOf(3L), EntryRanking.search(list, "MAIL.com").map { it.id })
    }

    @Test fun labelPrefixMatchesComeFirst() {
        val list = listOf(
            e(1, "Personal site", uses = 9, value = "port.dev"),
            e(2, "Portfolio"),
        )
        assertEquals(listOf(2L, 1L), EntryRanking.search(list, "port").map { it.id })
    }
}
