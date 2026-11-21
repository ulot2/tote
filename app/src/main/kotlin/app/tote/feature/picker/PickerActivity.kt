package app.tote.feature.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.MainActivity
import app.tote.core.data.settings.AppSettings
import app.tote.core.data.settings.SettingsRepository
import app.tote.ui.theme.ToteTheme
import app.tote.ui.theme.isAppInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Translucent sheet-style activity used by the Quick Settings tile (and, in Phase 4, by autofill's
 * "All entries…"). It runs in its own task so it never drags the main app forward.
 */
@AndroidEntryPoint
class PickerActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val viewModel: PickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        disableWindowAnimations()
        setContent {
            val settings by settingsRepository.settings
                .map<AppSettings, AppSettings?> { it }
                .collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent
            val state by viewModel.state.collectAsStateWithLifecycle()
            ToteTheme(darkTheme = isAppInDarkTheme(s.themeMode), dynamicColor = s.dynamicColor) {
                PickerScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onPick = viewModel::copy,
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    },
                    onFinished = ::finish,
                )
            }
        }
    }

    /** The sheet is a moment, not a place: leaving it (home, recents, screen off) closes it. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    /** The sheet animates itself; the window should just appear and vanish. */
    private fun disableWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        fun copyIntent(context: Context): Intent = Intent(context, PickerActivity::class.java)
    }
}
