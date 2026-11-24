package app.tote.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.tote.core.model.Entry
import app.tote.feature.vault.VaultScreen
import app.tote.feature.vault.VaultUiState
import app.tote.ui.nav.EditorRoute
import app.tote.ui.theme.ToteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1600dp")
class VaultScreenTest {

    @get:Rule val compose = createComposeRule()

    private val copied = mutableListOf<Entry>()
    private val deleted = mutableListOf<Entry>()
    private val restored = mutableListOf<Entry>()
    private val opened = mutableListOf<EditorRoute>()

    private val selectedProfiles = mutableListOf<Long?>()

    private fun show(state: VaultUiState) = compose.setContent {
        ToteTheme(darkTheme = true) {
            VaultScreen(
                state = state,
                onQueryChange = {},
                onCopy = { copied += it },
                onTogglePin = {},
                onDelete = { deleted += it },
                onRestore = { restored += it },
                onSetReordering = {},
                onCommitOrder = {},
                onOpenEditor = { opened += it },
                onOpenSettings = {},
                onSelectProfile = { selectedProfiles += it },
            )
        }
    }

    @Test fun tapCopiesAndShowsConfirmation() {
        show(SampleData.state())
        compose.onNodeWithText("GitHub").performClick()
        assertEquals(listOf(SampleData.github), copied)
        compose.onNodeWithText("Copied · GitHub").assertIsDisplayed()
    }

    @Test fun longPressOpensActionsAndDeleteCanBeUndone() {
        show(SampleData.state())
        compose.onNodeWithText("GitHub").performTouchInput { longClick() }
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertEquals(listOf(SampleData.github), deleted)
        compose.onNodeWithText("Undo").performClick()
        assertEquals(listOf(SampleData.github), restored)
    }

    @Test fun longPressEditOpensEditor() {
        show(SampleData.state())
        compose.onNodeWithText("Phone").performTouchInput { longClick() }
        compose.onNodeWithText("Edit").performClick()
        compose.waitForIdle()
        assertEquals(SampleData.phone.id, opened.single().id)
    }

    @Test fun pinnedEntriesAppearInRowAndSection() {
        show(SampleData.state())
        assertEquals(2, compose.onAllNodesWithText("LinkedIn").fetchSemanticsNodes().size)
    }

    @Test fun emptyVaultOffersTemplates() {
        show(SampleData.state(entries = emptyList()))
        compose.onNodeWithText("Your tote is empty").assertIsDisplayed()
        compose.onNodeWithText("Portfolio").performClick()
        assertEquals("portfolio", opened.single().template)
    }

    @Test fun fabOpensNewEntry() {
        show(SampleData.state())
        compose.onNodeWithContentDescription("New entry").performClick()
        assertEquals(0L, opened.single().id)
    }

    @Test fun reorderModeShowsHandles() {
        show(SampleData.state(reordering = true))
        compose.onNodeWithContentDescription("Reorder GitHub").assertIsDisplayed()
        compose.onAllNodesWithText("Done").onFirst().assertIsDisplayed()
    }

    @Test fun profileSwitcherShowsOnlyWithProfilesAndSelects() {
        show(SampleData.state())
        assertTrue(compose.onAllNodesWithText("Tech").fetchSemanticsNodes().isEmpty())
    }

    @Test fun profileSwitcherSelectsAndExplainsHiddenEntries() {
        val profiles = listOf(app.tote.core.model.Profile(1, "Tech"), app.tote.core.model.Profile(2, "Non-tech"))
        show(SampleData.state().copy(profiles = profiles, activeProfileId = 1, hiddenByProfile = 2))
        compose.onNodeWithText("2 entries from other profiles hidden").assertIsDisplayed()
        compose.onNodeWithText("Non-tech").performClick()
        compose.onNodeWithText("All").performClick()
        assertEquals(listOf<Long?>(2, null), selectedProfiles)
    }

    @Test fun searchWithNoMatchesSaysSo() {
        show(SampleData.state(query = "zzz"))
        compose.onNodeWithText("Nothing matches “zzz”").assertIsDisplayed()
    }
}
