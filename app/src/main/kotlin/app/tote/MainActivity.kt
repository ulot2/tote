package app.tote

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings
                .map<AppSettings, AppSettings?> { it }
                .collectAsStateWithLifecycle(initialValue = null)
            // Wait for the first settings read so a forced light/dark theme never flashes.
            val s = settings ?: return@setContent
            ready = true
            val dark = isAppInDarkTheme(s.themeMode)
            SystemBarsFor(dark)
            ToteTheme(darkTheme = dark, dynamicColor = s.dynamicColor) {
                ToteNavHost()
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
