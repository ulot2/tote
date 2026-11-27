package app.tote

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import app.tote.core.data.repo.EntryRepository
import app.tote.core.model.Entry
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.core.data.settings.AppSettings
import app.tote.core.data.settings.SettingsRepository
import app.tote.ui.nav.ToteNavHost
import app.tote.ui.theme.ToteTheme
import app.tote.ui.theme.isAppInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var entryRepository: EntryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        enableEdgeToEdge()
        setContent {
            val settingsFlow = remember { settingsRepository.settings.map<AppSettings, AppSettings?> { it } }
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val emptyFlow = remember { entryRepository.entries.map<List<Entry>, Boolean?> { it.isEmpty() } }
            val vaultEmpty by emptyFlow.collectAsStateWithLifecycle(initialValue = null)
            // Wait for the first settings and vault reads so a forced light/dark theme never
            // flashes and the right first screen shows.
            val s = settings ?: return@setContent
            val empty = vaultEmpty ?: return@setContent
            // Decided once: saving entries during onboarding mustn't swap the screen underneath.
            val showOnboarding = rememberSaveable { !s.onboardingDone && empty }
            LaunchedEffect(Unit) {
                // Someone who used Tuck before onboarding existed already has entries: never show it.
                if (!s.onboardingDone && !empty) settingsRepository.setOnboardingDone(true)
            }
            ready = true
            val dark = isAppInDarkTheme(s.themeMode)
            SystemBarsFor(dark)
            ToteTheme(darkTheme = dark, dynamicColor = s.dynamicColor, accent = s.accent) {
                ToteNavHost(showOnboarding = showOnboarding)
            }
        }
    }

    @Composable
    private fun SystemBarsFor(dark: Boolean) {
        DisposableEffect(dark) {
            val style = if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose { }
        }
    }
}
