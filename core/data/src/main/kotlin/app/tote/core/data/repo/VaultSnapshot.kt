package app.tote.core.data.repo

import app.tote.core.data.di.ApplicationScope
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Entry
import app.tote.core.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An always-warm, in-memory copy of the vault for services that must answer fast (Plan §9.4 step 8).
 * It starts listening to Room the first time it's injected and keeps the latest list for the
 * life of the process, so a fill request never waits on disk after the first one.
 */
@Singleton
class VaultSnapshot @Inject constructor(
    repository: EntryRepository,
    settings: SettingsRepository,
    profiles: ProfileRepository,
    learning: LearningRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    data class Snapshot(
        val entries: List<Entry>,
        val activeProfileId: Long?,
        val profiles: List<Profile> = emptyList(),
        /** (source, signature) → entry id → hits. */
        val learned: Map<Pair<String, String>, Map<Long, Int>> = emptyMap(),
    ) {
        val activeProfile: Profile? get() = profiles.firstOrNull { it.id == activeProfileId }

        fun learnedFor(source: String?, signature: String?): Map<Long, Int> =
            if (source == null || signature == null) emptyMap() else learned[source to signature].orEmpty()
    }

    val state: StateFlow<Snapshot?> = combine(
        repository.entries,
        settings.settings,
        profiles.profiles,
        learning.mappings,
    ) { entries, s, profileList, mappings ->
        Snapshot(
            entries = entries,
            // A stale id (profile deleted elsewhere) means "All".
            activeProfileId = s.activeProfileId?.takeIf { id -> profileList.any { it.id == id } },
            profiles = profileList,
            learned = mappings.groupBy { it.source to it.signature }.mapValues { (_, rows) -> rows.associate { it.entryId to it.hits } },
        )
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** The current snapshot, waiting for the first database read if needed. */
    suspend fun current(): Snapshot = state.filterNotNull().first()
}
