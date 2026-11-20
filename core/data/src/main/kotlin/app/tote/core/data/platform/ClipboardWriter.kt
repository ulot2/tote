package app.tote.core.data.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies entry values to the clipboard. Shared by the app, the tile and the picker.
 *
 * Vault values are links, contact details and stock answers, so they are explicitly marked as
 * non-sensitive (Plan §9.5). A future "private" entry flag would flip [sensitive].
 */
@Singleton
class ClipboardWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun copy(label: String, text: String, sensitive: Boolean = false) {
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, text)
        clip.description.extras = PersistableBundle().apply {
            val key = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                "android.content.extra.IS_SENSITIVE"
            }
            putBoolean(key, sensitive)
        }
        manager.setPrimaryClip(clip)
    }

    /** Plain text currently on the clipboard, or null. Only call on an explicit user action. */
    fun readText(): String? {
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    }
}
