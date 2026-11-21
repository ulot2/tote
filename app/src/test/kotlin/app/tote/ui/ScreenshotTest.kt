package app.tote.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.tote.feature.editor.EditorScreen
import app.tote.feature.editor.EditorUiState
import app.tote.feature.settings.SettingsScreen
import app.tote.feature.vault.VaultScreen
import app.tote.core.data.settings.AppSettings
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import app.tote.core.model.EntryValidator
import app.tote.ui.theme.ToteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Renders key screens in dark and light to PNGs under build/screenshots for visual review.
 * Not a pixel-diff test: it fails only if a screen crashes while rendering.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent { ToteTheme(darkTheme = dark) { content() } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        File(dir, "$name-${if (dark) "dark" else "light"}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun vault(dark: Boolean, state: app.tote.feature.vault.VaultUiState) = snap(
        if (state.isVaultEmpty) "vault-empty" else if (state.reordering) "vault-reorder" else "vault",
        dark,
    ) {
        VaultScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun vaultDark() = vault(true, SampleData.state())
    @Test fun vaultLight() = vault(false, SampleData.state())
    @Test fun vaultReorderDark() = vault(true, SampleData.state(reordering = true))
    @Test fun emptyDark() = vault(true, SampleData.state(entries = emptyList()))
    @Test fun emptyLight() = vault(false, SampleData.state(entries = emptyList()))

    private val editorState = EditorUiState(
        loading = false,
        isNew = false,
        label = "LinkedIn",
        value = "linkedin.com/in/toluwalope-adegoke",
        type = EntryType.LINK,
        category = Category.LINKS,
        icon = EntryIcon.LINKEDIN,
        keywords = listOf("linked in", "li profile"),
        validation = EntryValidator.Result(emptyList(), emptyList()),
    )

    private fun editor(dark: Boolean) = snap("editor", dark) {
        EditorScreen(editorState, "k", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun editorDark() = editor(true)
    @Test fun editorLight() = editor(false)

    @Test fun settingsDark() = snap("settings", true) { SettingsScreen(AppSettings(), {}, {}, {}, tilePromptSupported = true, onOpenChrome = {}) }

    @Test fun settingsAutofillOnLight() = snap("settings-autofill-on", false) {
        SettingsScreen(AppSettings(), {}, {}, {}, tilePromptSupported = true, autofillStatus = app.tote.util.AutofillSetup.Status.ENABLED, onOpenChrome = {})
    }

    @Test fun vaultBannerDark() = snap("vault-banner", true) {
        VaultScreen(SampleData.state(), {}, {}, {}, {}, {}, {}, {}, {}, {}, showAutofillBanner = true)
    }

    private fun picker(dark: Boolean, entries: List<app.tote.core.model.Entry>, name: String) = snap(name, dark) {
        // Draw something behind the translucent sheet, as the shade-launched picker would sit over another app.
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF3A5A7A)),
        )
        app.tote.feature.picker.PickerScreen(
            state = app.tote.feature.picker.PickerUiState(
                loading = false,
                entries = app.tote.core.model.EntryRanking.forPicker(entries),
                totalCount = entries.size,
            ),
            onQueryChange = {}, onPick = {}, onOpenApp = {}, onFinished = {}, startVisible = true,
        )
    }

    @Test fun pickerDark() = picker(true, SampleData.all, "picker")
    @Test fun pickerLight() = picker(false, SampleData.all, "picker")
    @Test fun pickerEmptyDark() = picker(true, emptyList(), "picker-empty")
}
