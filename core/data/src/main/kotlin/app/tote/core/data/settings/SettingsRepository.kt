package app.tote.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** Accent colour choices (Settings › Appearance). Iris is the default. */
enum class Accent { IRIS, EMBER, MARIGOLD, MINT, LIME, BONE }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You colours. Off by default: Tote has its own palette (Plan §8.2). */
    val dynamicColor: Boolean = false,
    val accent: Accent = Accent.IRIS,
    val onboardingDone: Boolean = false,
    /** null = the implicit "All" profile. */
    val activeProfileId: Long? = null,
    /** The user closed the "Turn on autofill" banner on the vault. */
    val autofillBannerDismissed: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val accent = stringPreferencesKey("accent")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val activeProfile = longPreferencesKey("active_profile_id")
        val autofillBannerDismissed = booleanPreferencesKey("autofill_banner_dismissed")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.dynamic] ?: false,
            accent = p[Keys.accent]?.let { name -> Accent.entries.firstOrNull { it.name == name } } ?: Accent.IRIS,
            onboardingDone = p[Keys.onboarding] ?: false,
            activeProfileId = p[Keys.activeProfile],
            autofillBannerDismissed = p[Keys.autofillBannerDismissed] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[Keys.dynamic] = enabled }
    }

    suspend fun setAccent(accent: Accent) {
        store.edit { it[Keys.accent] = accent.name }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[Keys.onboarding] = done }
    }

    suspend fun setAutofillBannerDismissed(dismissed: Boolean) {
        store.edit { it[Keys.autofillBannerDismissed] = dismissed }
    }

    suspend fun setActiveProfile(id: Long?) {
        store.edit { if (id == null) it.remove(Keys.activeProfile) else it[Keys.activeProfile] = id }
    }
}
