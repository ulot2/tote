package app.tote.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.tote.core.data.backup.BackupRepository
import app.tote.core.data.backup.ReadResult
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.data.repo.RoomEntryRepository
import app.tote.core.data.repo.RoomLearningRepository
import app.tote.core.data.repo.RoomProfileRepository
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbs = mutableListOf<ToteDatabase>()
    private val clock = Clock { 5_000L }

    private fun newDb() = Room.inMemoryDatabaseBuilder(context, ToteDatabase::class.java)
        .allowMainThreadQueries().build().also { dbs += it }

    @get:org.junit.Rule val tmp = org.junit.rules.TemporaryFolder()

    private fun kotlinx.coroutines.test.TestScope.settings() = app.tote.core.data.settings.SettingsRepository(
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("s${System.nanoTime()}.preferences_pb") },
    )

    @After fun tearDown() = dbs.forEach { it.close() }

    private fun entry(label: String, value: String, profiles: Set<Long> = emptySet(), keywords: List<String> = emptyList()) =
        Entry(label = label, value = value, type = EntryType.LINK, category = Category.LINKS, icon = EntryIcon.GLOBE, profileIds = profiles, keywords = keywords)

    @Test fun roundTripCarriesEntriesProfilesAndLearning() = runTest {
        val source = newDb()
        val profiles = RoomProfileRepository(source.profileDao(), source.entryDao(), settings())
        val tech = profiles.add("Tech")
        val entries = RoomEntryRepository(source.entryDao(), clock)
        val gh = entries.save(entry("GitHub", "github.com/sam", setOf(tech), listOf("code host")))
        entries.save(entry("Email", "sam@example.com"))
        entries.setPinned(gh, true)
        RoomLearningRepository(source.learnedDao(), clock).record("web:jobs.example", "sig-gh", gh)

        val text = BackupRepository(source, clock).export()

        val target = newDb()
        val repo = BackupRepository(target, clock)
        val backup = (repo.read(text) as ReadResult.Ok).backup
        val summary = repo.restore(backup)
        assertEquals(2, summary.entriesAdded)
        assertEquals(1, summary.profilesAdded)
        assertEquals(1, summary.learnedAdded)

        val restored = RoomEntryRepository(target.entryDao(), clock).entries.first()
        val newTech = target.profileDao().allOnce().single { it.name == "Tech" }.id
        val restoredGh = restored.single { it.label == "GitHub" }
        assertEquals("github.com/sam", restoredGh.value)
        assertEquals(setOf(newTech), restoredGh.profileIds)
        assertEquals(listOf("code host"), restoredGh.keywords)
        assertTrue(restoredGh.pinned)
        assertEquals(restoredGh.id, target.learnedDao().allOnce().single().entryId)
    }

    @Test fun importMergesAndSkipsWhatIsAlreadyThere() = runTest {
        val source = newDb()
        RoomProfileRepository(source.profileDao(), source.entryDao(), settings()).add("Tech")
        val entries = RoomEntryRepository(source.entryDao(), clock)
        entries.save(entry("GitHub", "github.com/sam"))
        entries.save(entry("Portfolio", "sam.dev"))
        val text = BackupRepository(source, clock).export()

        val target = newDb()
        RoomProfileRepository(target.profileDao(), target.entryDao(), settings()).add("tech")
        RoomEntryRepository(target.entryDao(), clock).save(entry("GitHub", "github.com/sam"))
        val repo = BackupRepository(target, clock)
        val backup = (repo.read(text) as ReadResult.Ok).backup

        val preview = repo.preview(backup)
        assertEquals(1, preview.entriesAdded)
        assertEquals(1, preview.entriesSkipped)
        assertEquals(0, preview.profilesAdded)

        repo.restore(backup)
        assertEquals(2, target.entryDao().allOnce().size)
        assertEquals(1, target.profileDao().allOnce().size)

        // Restoring the same file twice changes nothing.
        val again = repo.restore(backup)
        assertEquals(0, again.entriesAdded)
        assertEquals(2, target.entryDao().allOnce().size)
    }

    @Test fun duplicatesInsideTheFileAreAddedOnce() = runTest {
        val repo = BackupRepository(newDb(), clock)
        val text = """{"format":"tuck-vault","version":1,"exportedAt":1,"entries":[
            {"ref":1,"label":"Email","value":"a@b.c","type":"EMAIL","category":"CONTACT","icon":"MAIL"},
            {"ref":2,"label":"email","value":"a@b.c","type":"EMAIL","category":"CONTACT","icon":"MAIL"}]}"""
        val backup = (repo.read(text) as ReadResult.Ok).backup
        assertEquals(1, repo.preview(backup).entriesAdded)
        assertEquals(1, repo.restore(backup).entriesAdded)
    }

    @Test fun unknownEnumsAndExtraFieldsStillRestore() = runTest {
        val db = newDb()
        val repo = BackupRepository(db, clock)
        val text = """{"format":"tuck-vault","version":1,"exportedAt":1,"futureField":true,"entries":[
            {"ref":1,"label":"Mastodon","value":"@sam","type":"HANDLE","category":"SOCIAL","icon":"ELEPHANT","colour":"red"}]}"""
        val backup = (repo.read(text) as ReadResult.Ok).backup
        repo.restore(backup)
        val e = db.entryDao().allOnce().single()
        assertEquals(EntryType.SHORT_TEXT, e.type)
        assertEquals(Category.ANSWERS, e.category)
        assertEquals(EntryIcon.TEXT, e.icon)
    }

    @Test fun rejectsFilesThatAreNotBackups() {
        val repo = BackupRepository(newDb(), clock)
        assertEquals(ReadResult.NotABackup, repo.read("not json at all"))
        assertEquals(ReadResult.NotABackup, repo.read("""{"hello":"world"}"""))
        assertEquals(ReadResult.NotABackup, repo.read("""{"format":"other","version":1,"exportedAt":1}"""))
        assertEquals(ReadResult.NotABackup, repo.read("[1,2,3]"))
        assertEquals(ReadResult.TooNew(9), repo.read("""{"format":"tuck-vault","version":9,"exportedAt":1}"""))
    }
}
