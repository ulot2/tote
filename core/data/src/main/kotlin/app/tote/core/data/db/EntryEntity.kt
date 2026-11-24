package app.tote.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType

@Entity(
    tableName = "entries",
    indices = [Index("category", "sort_order")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val value: String,
    val type: EntryType,
    val category: Category,
    val icon: EntryIcon,
    @ColumnInfo(name = "icon_overridden") val iconOverridden: Boolean,
    /** Newline-separated user keywords. */
    val keywords: String,
    /** Comma-separated profile ids; empty = all profiles. */
    @ColumnInfo(name = "profile_ids") val profileIds: String,
    val pinned: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "use_count") val useCount: Int,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "accent_argb") val accentArgb: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

/**
 * "On this site/app, for this field, the user picked this entry" (Plan §6.3). Deleted with its entry.
 * [signature] is a normalised description of the field (see `FieldSignature`), never a typed value.
 */
@Entity(
    tableName = "learned_mappings",
    primaryKeys = ["source", "signature", "entry_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entry_id")],
)
data class LearnedMappingEntity(
    val source: String,
    val signature: String,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    val hits: Int,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long,
)
