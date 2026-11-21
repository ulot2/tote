package app.tote.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.data.repo.RoomEntryRepository
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomEntryRepositoryTest {

    private lateinit var db: ToteDatabase
    private lateinit var repo: RoomEntryRepository
    private var now = 1_000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ToteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomEntryRepository(db.entryDao(), Clock { now })
    }

    @After fun tearDown() = db.close()

    private fun entry(label: String, category: Category = Category.LINKS) = Entry(
        label = label,
        value = "$label.example",
        type = EntryType.LINK,
        category = category,
        icon = EntryIcon.GLOBE,
        keywords = listOf("one", " two ", ""),
        profileIds = setOf(3, 1),
    )

    @Test fun insertAssignsOrderAndTimestamps() = runTest {
        val a = repo.save(entry("a"))
        now = 2_000
        val b = repo.save(entry("b"))
        val c = repo.save(entry("c", Category.CONTACT))

        val loadedA = repo.get(a)!!
        assertEquals(0, loadedA.sortOrder)
        assertEquals(1_000, loadedA.createdAt)
        assertEquals(listOf("one", "two"), loadedA.keywords)
        assertEquals(setOf(1L, 3L), loadedA.profileIds)
        assertEquals(1, repo.get(b)!!.sortOrder)
        assertEquals(0, repo.get(c)!!.sortOrder)
    }

    @Test fun updateKeepsStatsAndCreatedAt() = runTest {
        val id = repo.save(entry("a"))
        repo.recordUse(id)
        now = 5_000
        repo.save(repo.get(id)!!.copy(label = "renamed", useCount = 0, createdAt = 0))

        val e = repo.get(id)!!
        assertEquals("renamed", e.label)
        assertEquals(1, e.useCount)
        assertEquals(1_000, e.createdAt)
        assertEquals(5_000, e.updatedAt)
    }

    @Test fun changingCategoryMovesToEnd() = runTest {
        repo.save(entry("x", Category.CONTACT))
        val id = repo.save(entry("a"))
        repo.save(repo.get(id)!!.copy(category = Category.CONTACT))
        assertEquals(1, repo.get(id)!!.sortOrder)
    }

    @Test fun reorderPersistsAndFlowEmits() = runTest {
        val a = repo.save(entry("a"))
        val b = repo.save(entry("b"))
        val c = repo.save(entry("c"))
        repo.reorder(listOf(c, a, b))
        repo.entries.test {
            assertEquals(listOf("c", "a", "b"), awaitItem().map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun pinDeleteRestore() = runTest {
        val id = repo.save(entry("a"))
        repo.setPinned(id, true)
        val pinned = repo.get(id)!!
        assertTrue(pinned.pinned)

        repo.delete(id)
        assertNull(repo.get(id))
        repo.restore(pinned)
        assertEquals(pinned, repo.get(id))
    }
}
