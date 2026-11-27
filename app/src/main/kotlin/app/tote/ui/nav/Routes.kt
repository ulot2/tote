package app.tote.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object VaultRoute

/**
 * @param id existing entry id, or 0 for a new entry.
 * @param template optional [app.tote.core.model.SeedTemplate] key to prefill a new entry.
 * @param origin which card the editor grew out of, for the shared-element transition.
 */
@Serializable
data class EditorRoute(val id: Long = 0L, val template: String? = null, val origin: String = ORIGIN_LIST) {
    companion object {
        const val ORIGIN_LIST = "list"
        const val ORIGIN_PINNED = "pinned"
        const val ORIGIN_FAB = "fab"
    }
}

@Serializable
data object SettingsRoute

@Serializable
data object ProfilesRoute

@Serializable
data object LearnedRoute

/** First run only; see `MainActivity`. */
@Serializable
data object OnboardingRoute

/** Shared-element keys. The same key on two screens makes Compose morph one into the other. */
object SharedKeys {
    fun entry(id: Long, origin: String) = when (origin) {
        EditorRoute.ORIGIN_FAB -> "new-entry"
        EditorRoute.ORIGIN_PINNED -> "pinned-$id"
        else -> "entry-$id"
    }
}
