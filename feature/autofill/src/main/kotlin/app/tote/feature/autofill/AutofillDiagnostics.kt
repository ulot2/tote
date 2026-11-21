package app.tote.feature.autofill

/**
 * What the last fill request did, kept in memory so the "Try autofill" screen can show it.
 * Counts, timing and an error class name only: never entry values, field text or the app's name.
 */
object AutofillDiagnostics {

    data class LastRequest(
        val atMillis: Long,
        val fields: Int,
        val suggestions: Int,
        val durationMs: Long,
        /** Exception class name when the request failed, e.g. "IllegalStateException". */
        val error: String? = null,
    )

    @Volatile
    var last: LastRequest? = null
        private set

    internal fun record(request: LastRequest) {
        last = request
    }
}
