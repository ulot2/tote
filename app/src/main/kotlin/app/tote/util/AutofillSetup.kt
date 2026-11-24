package app.tote.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Whether Tuck is the autofill service, and how to make it so (Plan §8.4, §9.7). */
object AutofillSetup {

    const val CHROME_PACKAGE = "com.android.chrome"

    enum class Status { ENABLED, DISABLED, UNSUPPORTED }

    fun status(context: Context): Status {
        val manager = context.getSystemService(AutofillManager::class.java) ?: return Status.UNSUPPORTED
        if (!manager.isAutofillSupported) return Status.UNSUPPORTED
        return if (manager.hasEnabledAutofillServices()) Status.ENABLED else Status.DISABLED
    }

    /** Opens the system prompt that makes Tuck the autofill service. */
    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).setData(Uri.parse("package:${context.packageName}"))

    /** The system screen listing autofill services, for switching back to a password manager. */
    fun manageIntent(): Intent = Intent(Settings.ACTION_SETTINGS)

    fun chromeLaunchIntent(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(CHROME_PACKAGE)
}

/** Autofill status, re-read whenever the screen resumes (the user may have just changed it). */
@Composable
fun rememberAutofillStatus(): AutofillSetup.Status {
    val context = LocalContext.current
    var status by remember { mutableStateOf(AutofillSetup.status(context)) }
    LifecycleResumeEffect(Unit) {
        status = AutofillSetup.status(context)
        onPauseOrDispose { }
    }
    return status
}
