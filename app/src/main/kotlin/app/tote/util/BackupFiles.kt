package app.tote.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Reads and writes backup files the user picked through the system file picker. */
object BackupFiles {
    /** Far larger than any real vault; stops a wrong pick (a video, say) from filling memory. */
    const val MAX_BYTES = 8 * 1024 * 1024

    suspend fun write(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Can't open $uri")
        stream.use { it.write(text.toByteArray()) }
    }

    /** The file's text, or null when it can't be read or is too big to be a backup. */
    suspend fun read(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                while (out.size() <= MAX_BYTES) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
                if (out.size() > MAX_BYTES) null else out.toByteArray().decodeToString()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** MIME types to offer when picking a backup; some file managers label JSON as text or binary. */
    val OPEN_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")
}
