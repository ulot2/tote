package app.tote.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    /** Light tick for a successful copy. */
    fun tick(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(constant)
    }

    fun longPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Drag start/stop and segment changes. */
    fun segment(view: View) {
        val constant = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(constant)
    }
}
