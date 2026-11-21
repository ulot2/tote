package app.tote.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import app.tote.core.data.platform.ClipboardWriter
import app.tote.core.data.repo.EntryRepository
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.feature.editor.EditorViewModel
import app.tote.ui.nav.EditorRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeRepo : EntryRepository {
    val store = MutableStateFlow<List<Entry>>(emptyList())
    override val entries: Flow<List<Entry>> = store
    override fun observe(id: Long) = store.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Long) = store.value.firstOrNull { it.id == id }
    override suspend fun save(entry: Entry): Long {
        val id = if (entry.id == 0L) (store.value.maxOfOrNull { it.id } ?: 0) + 1 else entry.id
        store.value = store.value.filterNot { it.id == id } + entry.copy(id = id)
        return id
    }
    override suspend fun delete(id: Long) { store.value = store.value.filterNot { it.id == id } }
    override suspend fun restore(entry: Entry) { store.value = store.value + entry }
    override suspend fun setPinned(id: Long, pinned: Boolean) = Unit
    override suspend fun recordUse(id: Long) = Unit
    override suspend fun reorder(orderedIds: List<Long>) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EditorViewModelTest {

    private val repo = FakeRepo()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(route: EditorRoute) = EditorViewModel(
        SavedStateHandle(mapOf("id" to route.id, "template" to route.template, "origin" to route.origin)),
        repo,
        ClipboardWriter(ApplicationProvider.getApplicationContext()),
    )

    @Test fun typingALinkedinUrlAutoDetectsEverything() {
        val vm = vm(EditorRoute())
        vm.onLabelChange("LinkedIn")
        vm.onValueChange("linkedin.com/in/someone")
        val s = vm.state.value
        assertEquals(EntryType.LINK, s.type)
        assertEquals(Category.LINKS, s.category)
        assertEquals(EntryIcon.LINKEDIN, s.icon)
    }

    @Test fun manualTypeStopsAutoDetection() {
        val vm = vm(EditorRoute())
        vm.onTypeSelected(EntryType.SHORT_TEXT)
        vm.onValueChange("someone@example.com")
        assertEquals(EntryType.SHORT_TEXT, vm.state.value.type)
    }

    @Test fun saveWithBlanksShowsErrorsAndDoesNotPersist() {
        val vm = vm(EditorRoute())
        vm.save()
        assertTrue(vm.state.value.showErrors)
        assertFalse(vm.state.value.finished)
        assertTrue(repo.store.value.isEmpty())
    }

    @Test fun templatePrefillsAndSaves() {
        val vm = vm(EditorRoute(template = "cv"))
        assertEquals("CV link", vm.state.value.label)
        vm.onValueChange("https://drive.google.com/file/d/x/view")
        vm.onKeywordDraftChange("resume,")
        vm.save()
        val saved = repo.store.value.single()
        assertEquals(Category.DOCUMENTS, saved.category)
        assertEquals(EntryIcon.GOOGLE_DRIVE, saved.icon)
        assertEquals(listOf("resume"), saved.keywords)
        assertTrue(vm.state.value.finished)
    }

    @Test fun editingKeepsManualIconOverride() = kotlinx.coroutines.test.runTest {
        val id = repo.save(Entry(label = "Site", value = "github.com/me", type = EntryType.LINK, category = Category.LINKS, icon = EntryIcon.GLOBE, iconOverridden = true))
        val vm = vm(EditorRoute(id = id))
        vm.onValueChange("github.com/other")
        assertEquals(EntryIcon.GLOBE, vm.state.value.icon)
    }
}
