package app.tote.core.data.repo

import app.tote.core.data.db.EntryDao
import app.tote.core.data.db.ProfileDao
import app.tote.core.data.db.toEntity
import app.tote.core.data.db.toModel
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Profiles such as "Tech" and "Non-tech" (Plan §6.2). "All" is implicit and never stored. */
interface ProfileRepository {
    val profiles: Flow<List<Profile>>

    /** Returns the new profile's id. */
    suspend fun add(name: String): Long
    suspend fun rename(id: Long, name: String)

    /** Deletes the profile, takes it off every entry, and falls back to "All" if it was active. */
    suspend fun delete(id: Long)
}

@Singleton
class RoomProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val entryDao: EntryDao,
    private val settings: SettingsRepository,
) : ProfileRepository {

    override val profiles: Flow<List<Profile>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun add(name: String): Long {
        val order = profiles.first().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        return dao.insert(Profile(name = name.trim(), sortOrder = order).toEntity())
    }

    override suspend fun rename(id: Long, name: String) {
        val current = profiles.first().firstOrNull { it.id == id } ?: return
        dao.update(current.copy(name = name.trim()).toEntity())
    }

    override suspend fun delete(id: Long) {
        // Entries that belonged only to this profile go back to "All" rather than vanishing.
        entryDao.allOnce().filter { e -> id.toString() in e.profileIds.split(',') }.forEach { e ->
            val remaining = e.profileIds.split(',').filter { it.isNotBlank() && it != id.toString() }.joinToString(",")
            entryDao.update(e.copy(profileIds = remaining))
        }
        dao.delete(id)
        if (settings.settings.first().activeProfileId == id) settings.setActiveProfile(null)
    }
}
