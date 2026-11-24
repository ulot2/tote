package app.tote.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.data.repo.RoomEntryRepository
import app.tote.core.data.repo.RoomLearningRepository
import app.tote.core.data.repo.RoomProfileRepository
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LearningAndProfilesTest {

    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val migrations = MigrationTestHelper(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        ToteDatabase::class.java,
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: ToteDatabase
    private var now = 1_000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, ToteDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun settings(scope: TestScope) = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("s${System.nanoTime()}.preferences_pb") },
    )

    private fun entry(label: String, profiles: Set<Long> = emptySet()) =
        Entry(label = label, value = "$label.dev", type = EntryType.LINK, category = Category.LINKS, icon = EntryIcon.GLOBE, profileIds = profiles)

    @Test fun learningCountsPicksAndForgets() = runTest {
        val entries = RoomEntryRepository(db.entryDao(), Clock { now })
        val learning = RoomLearningRepository(db.learnedDao(), Clock { now })
        val a = entries.save(entry("a"))
        val b = entries.save(entry("b"))

        learning.record("jobs.lever.co", "label:website", a)
        now = 2_000
        learning.record("jobs.lever.co", "label:website", a)
        learning.record("jobs.lever.co", "label:website", b)
        learning.record("boards.greenhouse.io", "label:website", b)

        val rows = learning.mappings.first()
        assertEquals(2, rows.first { it.source == "jobs.lever.co" && it.entryId == a }.hits)
        assertEquals(2_000, rows.first { it.source == "jobs.lever.co" && it.entryId == a }.lastUsedAt)

        learning.forget("jobs.lever.co", "label:website")
        assertEquals(listOf("boards.greenhouse.io"), learning.mappings.first().map { it.source })

        // Deleting an entry deletes what was learned about it.
        entries.delete(b)
        assertTrue(learning.mappings.first().isEmpty())
    }

    @Test fun deletingAProfileReturnsItsEntriesToAllAndResetsTheActiveProfile() = runTest {
        val s = settings(this)
        val profiles = RoomProfileRepository(db.profileDao(), db.entryDao(), s)
        val entries = RoomEntryRepository(db.entryDao(), Clock { now })
        val tech = profiles.add(" Tech ")
        val other = profiles.add("Non-tech")
        val onlyTech = entries.save(entry("cv", setOf(tech)))
        val both = entries.save(entry("bio", setOf(tech, other)))
        s.setActiveProfile(tech)

        assertEquals(listOf("Tech", "Non-tech"), profiles.profiles.first().map { it.name })
        profiles.delete(tech)

        assertTrue(entries.get(onlyTech)!!.isInAllProfiles)
        assertEquals(setOf(other), entries.get(both)!!.profileIds)
        assertNull(s.settings.first().activeProfileId)
    }

    @Test fun migratesVersion1VaultsWithoutLosingEntries() {
        val name = "migration-test.db"
        migrations.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO entries (id, label, value, type, category, icon, icon_overridden, keywords, profile_ids, pinned, sort_order, use_count, last_used_at, created_at, updated_at) " +
                    "VALUES (1, 'LinkedIn', 'linkedin.com/in/x', 'LINK', 'LINKS', 'LINKEDIN', 0, '', '', 1, 0, 3, NULL, 1, 1)",
            )
            close()
        }
        val v2 = migrations.runMigrationsAndValidate(name, 2, true)
        v2.query("SELECT label, use_count FROM entries").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("LinkedIn", c.getString(0))
            assertEquals(3, c.getInt(1))
        }
        v2.query("SELECT COUNT(*) FROM learned_mappings").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
    }
}
