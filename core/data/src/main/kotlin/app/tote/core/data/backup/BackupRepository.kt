package app.tote.core.data.backup

import androidx.room.withTransaction
import app.tote.core.data.db.EntryEntity
import app.tote.core.data.db.ProfileEntity
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** What a restore would do, shown before the user confirms, and what it did after. */
data class RestoreSummary(
    val entriesAdded: Int,
    val entriesSkipped: Int,
    val profilesAdded: Int,
    val learnedAdded: Int,
)

sealed interface ReadResult {
    data class Ok(val backup: VaultBackup) : ReadResult
    /** Not JSON, or JSON that isn't a Tuck backup. */
    data object NotABackup : ReadResult
    /** A Tuck backup from a newer version of the format than this app reads. */
    data class TooNew(val version: Int) : ReadResult
}

/**
 * Export and import (Plan §5 P2). Import **merges** and never deletes: entries the vault already
 * has (same label and value) are skipped, profiles are matched by name, and learned picks follow
 * their entries.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val db: ToteDatabase,
    private val clock: Clock,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun export(): String {
        val entries = db.entryDao().allOnce()
        val profiles = db.profileDao().allOnce()
        val learned = db.learnedDao().allOnce()
        val backup = VaultBackup(
            exportedAt = clock.now(),
            profiles = profiles.map { BackupProfile(it.id, it.name, it.sortOrder) },
            entries = entries.map { e ->
                BackupEntry(
                    ref = e.id,
                    label = e.label,
                    value = e.value,
                    type = e.type.name,
                    category = e.category.name,
                    icon = e.icon.name,
                    iconOverridden = e.iconOverridden,
                    keywords = e.keywords.split('\n').filter { it.isNotBlank() },
                    profileRefs = e.profileIds.split(',').mapNotNull { it.trim().toLongOrNull() },
                    pinned = e.pinned,
                    sortOrder = e.sortOrder,
                    useCount = e.useCount,
                    lastUsedAt = e.lastUsedAt,
                    createdAt = e.createdAt,
                    updatedAt = e.updatedAt,
                )
            },
            learned = learned.map { BackupLearned(it.source, it.signature, it.entryId, it.hits, it.lastUsedAt) },
        )
        return json.encodeToString(VaultBackup.serializer(), backup)
    }

    fun read(text: String): ReadResult {
        val backup = try {
            json.decodeFromString(VaultBackup.serializer(), text)
        } catch (e: SerializationException) {
            return ReadResult.NotABackup
        } catch (e: IllegalArgumentException) {
            return ReadResult.NotABackup
        }
        if (backup.format != VaultBackup.FORMAT) return ReadResult.NotABackup
        if (backup.version > VaultBackup.VERSION) return ReadResult.TooNew(backup.version)
        return ReadResult.Ok(backup)
    }

    /** What [restore] would do, without changing anything. */
    suspend fun preview(backup: VaultBackup): RestoreSummary = plan(backup).summary

    suspend fun restore(backup: VaultBackup): RestoreSummary {
        return db.withTransaction {
            val plan = plan(backup)
            // Profiles: reuse by name, else create.
            val profileIds = HashMap<Long, Long>()
            var nextProfileOrder = (db.profileDao().allOnce().maxOfOrNull { it.sortOrder } ?: -1) + 1
            for (p in backup.profiles) {
                val existing = plan.existingProfiles[p.name.trim().lowercase()]
                profileIds[p.ref] = existing ?: db.profileDao().insert(ProfileEntity(name = p.name.trim(), accentArgb = null, sortOrder = nextProfileOrder++))
            }
            // Entries: skip ones already in the vault, add the rest at the end of their section.
            val entryIds = HashMap<Long, Long>()
            val known = HashMap(plan.existingEntries)
            val nextOrder = HashMap<Category, Int>()
            for (e in backup.entries) {
                known[key(e.label, e.value)]?.let { entryIds[e.ref] = it; continue }
                val category = enumOr(e.category, Category.ANSWERS)
                val order = nextOrder.getOrPut(category) { db.entryDao().maxSortOrder(category) + 1 }
                nextOrder[category] = order + 1
                val now = clock.now()
                entryIds[e.ref] = db.entryDao().insert(
                    EntryEntity(
                        label = e.label.trim(),
                        value = e.value,
                        type = enumOr(e.type, EntryType.SHORT_TEXT),
                        category = category,
                        icon = enumOr(e.icon, EntryIcon.TEXT),
                        iconOverridden = e.iconOverridden,
                        keywords = e.keywords.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n"),
                        profileIds = e.profileRefs.mapNotNull { profileIds[it] }.sorted().joinToString(","),
                        pinned = e.pinned,
                        sortOrder = order,
                        useCount = e.useCount,
                        lastUsedAt = e.lastUsedAt,
                        createdAt = e.createdAt.takeIf { it > 0 } ?: now,
                        updatedAt = now,
                    ),
                ).also { known[key(e.label, e.value)] = it }
            }
            var learnedAdded = 0
            for (l in backup.learned) {
                val entryId = entryIds[l.entryRef] ?: continue
                if (db.learnedDao().insertIfMissing(l.source, l.signature, entryId, l.hits, l.lastUsedAt) != -1L) learnedAdded++
            }
            plan.summary.copy(learnedAdded = learnedAdded)
        }
    }

    private class Plan(
        val existingEntries: Map<String, Long>,
        val existingProfiles: Map<String, Long>,
        val summary: RestoreSummary,
    )

    private suspend fun plan(backup: VaultBackup): Plan {
        val existingEntries = db.entryDao().allOnce().associate { key(it.label, it.value) to it.id }
        val existingProfiles = db.profileDao().allOnce().associate { it.name.trim().lowercase() to it.id }
        val skipped = backup.entries.count { key(it.label, it.value) in existingEntries }
        // Duplicates inside the file itself only count once.
        val added = backup.entries.filterNot { key(it.label, it.value) in existingEntries }.distinctBy { key(it.label, it.value) }.size
        return Plan(
            existingEntries = existingEntries,
            existingProfiles = existingProfiles,
            summary = RestoreSummary(
                entriesAdded = added,
                entriesSkipped = skipped,
                profilesAdded = backup.profiles.count { it.name.trim().lowercase() !in existingProfiles },
                learnedAdded = backup.learned.size,
            ),
        )
    }

    private fun key(label: String, value: String) = label.trim().lowercase() + "\u0000" + value.trim()

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    companion object {
        const val MIME_TYPE = "application/json"
        fun fileName(date: java.time.LocalDate): String = "tuck-backup-$date.json"
    }
}
