package app.tote.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tote.core.model.Entry
import app.tote.core.model.EntryRanking
import app.tote.feature.picker.PickerScreen
import app.tote.feature.picker.PickerUiState
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
class PickerScreenTest {

    @get:Rule val compose = createComposeRule()

    private val picked = mutableListOf<Entry>()
    private var finished = 0
    private var openedApp = 0

    private fun show(state: PickerUiState) = compose.setContent {
        ToteTheme(darkTheme = true) {
            PickerScreen(
                state = state,
                onQueryChange = {},
                onPick = { picked += it },
                onOpenApp = { openedApp++ },
                onFinished = { finished++ },
                startVisible = true,
            )
        }
    }

    private fun stateOf(entries: List<Entry>, query: String = "") =
        PickerUiState(loading = false, query = query, entries = EntryRanking.search(entries, query), totalCount = entries.size)

    @Test fun pickedEntryIsCopiedOnceThenSheetCloses() {
        compose.mainClock.autoAdvance = false
        show(stateOf(SampleData.all))
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("GitHub").performClick()
        compose.onNodeWithText("GitHub").performClick() // a second tap must not copy again
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithContentDescription("Copied").assertIsDisplayed()
        assertEquals(listOf(SampleData.github), picked)
        assertEquals(0, finished)

        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(1, finished)
    }

    @Test fun pinnedEntriesAreListedFirst() {
        val order = stateOf(SampleData.all).entries.map { it.label }
        assertTrue(order.take(3).containsAll(listOf("LinkedIn", "Portfolio", "Email")))
    }

    @Test fun noMatches() {
        show(stateOf(SampleData.all, query = "zzz"))
        compose.onNodeWithText("Nothing matches “zzz”").assertIsDisplayed()
    }

    @Test fun emptyVaultOffersToOpenApp() {
        show(stateOf(emptyList()))
        compose.onNodeWithText("Your tote is empty").assertIsDisplayed()
        compose.onNodeWithText("Open Tote").performClick()
        assertEquals(1, openedApp)
    }

    @Test fun closeButtonDismisses() {
        show(stateOf(SampleData.all))
        compose.onNodeWithContentDescription("Close").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(1, finished)
        assertTrue(picked.isEmpty())
    }
}
