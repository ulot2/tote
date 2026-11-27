package app.tote.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import app.tote.core.data.backup.RestoreSummary
import app.tote.core.data.settings.AppSettings
import app.tote.feature.onboarding.OnboardingScreen
import app.tote.feature.onboarding.OnboardingState
import app.tote.feature.onboarding.OnboardingStep
import app.tote.feature.settings.BackupUi
import app.tote.feature.settings.SettingsScreen
import app.tote.feature.vault.VaultScreen
import app.tote.ui.theme.ToteTheme
import app.tote.util.AutofillSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Onboarding, backup and restore, and the large-font pass: renders to build/screenshots.
 * "-large" shots use font scale 2.0 (Android's largest), applied linearly, which is harsher
 * than the platform's non-linear scaling, so if these hold up the real thing does too.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingScreenshotTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun snap(name: String, dark: Boolean = true, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ToteTheme(darkTheme = dark) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        val suffix = (if (fontScale > 1f) "-large" else "") + if (dark) "-dark" else "-light"
        File(dir, "$name$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable
    private fun onboarding(state: OnboardingState, status: AutofillSetup.Status = AutofillSetup.Status.DISABLED) = OnboardingScreen(
        state = state,
        onBack = {}, onStart = {}, onRestore = {}, onToggle = {}, onContinuePick = {}, onSkipPick = {},
        onValue = { _, _ -> }, onSaveValues = {},
        autofillStatus = status, onEnableAutofill = {},
        tilePromptSupported = true, tileResult = null, onAddTile = {}, onFinish = {},
    )

    private val filled = OnboardingState(
        step = OnboardingStep.FILL,
        values = mapOf("linkedin" to "linkedin.com/in/sam-rivera", "email" to "sam@example.com"),
    )

    @Test fun welcome() = snap("onboarding-1-welcome") { onboarding(OnboardingState()) }
    @Test fun welcomeLight() = snap("onboarding-1-welcome", dark = false) { onboarding(OnboardingState()) }
    @Test fun welcomeRestoreError() = snap("onboarding-1-welcome-error") {
        onboarding(OnboardingState(restoreError = "That isn't a Tuck backup. Pick the .json file Tuck saved."))
    }
    @Test fun pick() = snap("onboarding-2-pick") { onboarding(OnboardingState(step = OnboardingStep.PICK)) }
    @Test fun pickLight() = snap("onboarding-2-pick", dark = false) { onboarding(OnboardingState(step = OnboardingStep.PICK)) }
    @Test fun fill() = snap("onboarding-3-fill") { onboarding(filled) }
    @Test fun autofill() = snap("onboarding-4-autofill") { onboarding(OnboardingState(step = OnboardingStep.AUTOFILL)) }
    @Test fun autofillOn() = snap("onboarding-4-autofill-on", dark = false) {
        onboarding(OnboardingState(step = OnboardingStep.AUTOFILL), AutofillSetup.Status.ENABLED)
    }

    @Test fun restoreConfirm() = snap("settings-restore-confirm") {
        SettingsScreen(AppSettings(), {}, {}, {}, backup = BackupUi.ConfirmRestore(RestoreSummary(12, 3, 1, 4)))
    }
    @Test fun restoreFailed() = snap("settings-restore-failed", dark = false) {
        SettingsScreen(AppSettings(), {}, {}, {}, backup = BackupUi.Failed("That isn't a Tuck backup", "Pick the .json file Tuck saved when you backed up."))
    }
    @Test fun backupDone() = snap("settings-backup-done") {
        SettingsScreen(AppSettings(), {}, {}, {}, backup = BackupUi.Done("Backed up 14 entries"))
    }

    // Large font (Plan §11 accessibility).
    @Test fun vaultLarge() = snap("vault", fontScale = 2f) { VaultScreen(SampleData.state(), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
    @Test fun emptyLarge() = snap("vault-empty", fontScale = 2f) { VaultScreen(SampleData.state(entries = emptyList()), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
    @Test fun settingsLarge() = snap("settings", fontScale = 2f) { SettingsScreen(AppSettings(), {}, {}, {}, tilePromptSupported = true, onOpenChrome = {}) }
    @Test fun pickLarge() = snap("onboarding-2-pick", fontScale = 2f) { onboarding(OnboardingState(step = OnboardingStep.PICK)) }
    @Test fun welcomeLarge() = snap("onboarding-1-welcome", fontScale = 2f) { onboarding(OnboardingState()) }
    @Test fun autofillLarge() = snap("onboarding-4-autofill", fontScale = 2f) { onboarding(OnboardingState(step = OnboardingStep.AUTOFILL)) }
    @Test fun pickerLarge() = snap("picker", fontScale = 2f) {
        app.tote.feature.picker.PickerScreen(
            state = app.tote.feature.picker.PickerUiState(
                loading = false,
                entries = app.tote.core.model.EntryRanking.forPicker(SampleData.all),
                totalCount = SampleData.all.size,
            ),
            onQueryChange = {}, onPick = {}, onOpenApp = {}, onFinished = {}, startVisible = true,
        )
    }
}
