package app.tote.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.tote.core.data.backup.BackupRepository
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.data.repo.RoomEntryRepository
import app.tote.core.data.settings.SettingsRepository
import app.tote.feature.onboarding.OnboardingStep
import app.tote.feature.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @get:Rule val tmp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: ToteDatabase

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(context, ToteDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.settings() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("s${System.nanoTime()}.preferences_pb") },
    )

    /** Room works on its own threads; wait for the save to land. */
    private suspend fun OnboardingViewModel.awaitSaved() = state.first { it.step == OnboardingStep.AUTOFILL && !it.busy }

    @Test fun savesFilledValuesAndSkipsBlankOnes() = runTest {
        val entries = RoomEntryRepository(db.entryDao(), Clock { 1L })
        val vm = OnboardingViewModel(entries, settings(), BackupRepository(db, Clock { 1L }), context)
        vm.go(OnboardingStep.FILL)
        vm.setValue("linkedin", "  linkedin.com/in/someone ")
        vm.setValue("email", "someone@example.com")
        vm.setValue("phone", "+1 555 0100") // not selected by default: ignored
        vm.saveValues()
        vm.awaitSaved()

        val saved = db.entryDao().allOnce().sortedBy { it.label }
        assertEquals(listOf("Email", "LinkedIn"), saved.map { it.label })
        assertEquals("linkedin.com/in/someone", saved.last().value)
        assertEquals(OnboardingStep.AUTOFILL, vm.state.value.step)
    }

    @Test fun goingBackAndSavingAgainEditsInsteadOfDuplicating() = runTest {
        val entries = RoomEntryRepository(db.entryDao(), Clock { 1L })
        val vm = OnboardingViewModel(entries, settings(), BackupRepository(db, Clock { 1L }), context)
        vm.setValue("github", "github.com/old")
        vm.setValue("email", "a@example.com")
        vm.saveValues()
        vm.awaitSaved()
        assertTrue(vm.back())
        vm.setValue("github", "github.com/new")
        vm.toggle("email") // deselected after saving: removed again
        vm.saveValues()
        vm.awaitSaved()

        val saved = db.entryDao().allOnce()
        assertEquals(1, saved.size)
        assertEquals("github.com/new", saved.single().value)
    }

    @Test fun finishMarksOnboardingDone() = runTest {
        val s = settings()
        val vm = OnboardingViewModel(RoomEntryRepository(db.entryDao(), Clock { 1L }), s, BackupRepository(db, Clock { 1L }), context)
        assertFalse(vm.back())
        vm.finish()
        vm.state.first { it.finished }
        assertTrue(s.settings.first().onboardingDone)
        assertTrue(vm.state.value.finished)
    }
}
