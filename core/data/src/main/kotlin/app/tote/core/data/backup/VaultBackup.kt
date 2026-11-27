package app.tote.core.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The backup file (Plan §5 P2: export or import the vault as JSON, user-initiated).
 *
 * Plain, readable JSON on purpose: the user owns it and can open it. Enums are stored by name,
 * and unknown names fall back on import, so a newer file still restores into an older app.
 * The [format] marker lets Tuck reject a JSON file that isn't a Tuck backup.
 */
@Serializable
data class VaultBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val app: String = "Tuck",
    val profiles: List<BackupProfile> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
    /** What Tuck learned, keyed to entries by their [BackupEntry.ref]. */
    val learned: List<BackupLearned> = emptyList(),
) {
    companion object {
        const val FORMAT = "tuck-vault"
        const val VERSION = 1
    }
}

@Serializable
data class BackupProfile(
    /** Id in the exporting vault; only used to link entries to profiles inside this file. */
    val ref: Long,
    val name: String,
    val sortOrder: Int = 0,
)

@Serializable
data class BackupEntry(
    /** Id in the exporting vault; links learned picks to entries inside this file. */
    val ref: Long,
    val label: String,
    val value: String,
    val type: String,
    val category: String,
    val icon: String,
    val iconOverridden: Boolean = false,
    val keywords: List<String> = emptyList(),
    /** [BackupProfile.ref]s; empty = all profiles. */
    @SerialName("profiles") val profileRefs: List<Long> = emptyList(),
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupLearned(
    val source: String,
    val signature: String,
    val entryRef: Long,
    val hits: Int = 1,
    val lastUsedAt: Long = 0,
)
