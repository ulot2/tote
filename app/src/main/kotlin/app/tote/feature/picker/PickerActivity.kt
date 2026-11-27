package app.tote.feature.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import app.tote.feature.autofill.AutofillResults
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.MainActivity
import app.tote.core.data.settings.AppSettings
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Entry
import app.tote.ui.theme.ToteTheme
import app.tote.ui.theme.isAppInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Translucent sheet-style activity used by the Quick Settings tile. It runs in its own task so it
 * never drags the main app forward. [AutofillPickerActivity] reuses it for autofill's "All entries…".
 */
@AndroidEntryPoint
open class PickerActivity : ComponentActivity() {

    protected open val mode: PickerMode = PickerMode.COPY

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
            val settingsFlow = remember { settingsRepository.settings.map<AppSettings, AppSettings?> { it } }
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent
            val state by viewModel.state.collectAsStateWithLifecycle()
            ToteTheme(darkTheme = isAppInDarkTheme(s.themeMode), dynamicColor = s.dynamicColor, accent = s.accent) {
                PickerScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onPick = ::onPick,
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    },
                    onFinished = ::finish,
                    mode = mode,
                    onSelectProfile = viewModel::selectProfile,
                )
            }
        }
    }

    protected open fun onPick(entry: Entry) = viewModel.copy(entry)

    protected fun recordUse(entry: Entry) = viewModel.recordUse(entry)

    protected fun learn(source: String, signature: String, entry: Entry) = viewModel.learn(source, signature, entry)

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

/**
 * The picker opened by autofill's "All entries…" (dataset authentication). A separate, standard
 * launch-mode activity: the system delivers its result back to the app being filled, which a
 * single-task activity in its own task can't do.
 */
@AndroidEntryPoint
class AutofillPickerActivity : PickerActivity() {

    override val mode: PickerMode = PickerMode.FILL

    override fun onPick(entry: Entry) {
        val id = AutofillResults.autofillId(intent) ?: return
        val urlField = intent.getBooleanExtra(AutofillResults.EXTRA_URL_FIELD, false)
        // Set the result now; the sheet animates out and then finishes, delivering it.
        setResult(RESULT_OK, AutofillResults.resultFor(this, id, entry, urlField))
        recordUse(entry)
        val source = intent.getStringExtra(AutofillResults.EXTRA_SOURCE)
        val signature = intent.getStringExtra(AutofillResults.EXTRA_SIGNATURE)
        if (source != null && signature != null) learn(source, signature, entry)
    }

    companion object {
        fun fillIntent(context: Context, autofillId: Parcelable, urlField: Boolean, source: String? = null, signature: String? = null): Intent =
            Intent(context, AutofillPickerActivity::class.java)
                .putExtra(AutofillResults.EXTRA_AUTOFILL_ID, autofillId)
                .putExtra(AutofillResults.EXTRA_URL_FIELD, urlField)
                .putExtra(AutofillResults.EXTRA_SOURCE, source)
                .putExtra(AutofillResults.EXTRA_SIGNATURE, signature)
    }
}
