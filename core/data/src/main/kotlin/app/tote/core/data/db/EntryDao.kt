package app.tote.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.tote.core.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY category, sort_order, id")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observe(id: Long): Flow<EntryEntity?>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM entries WHERE category = :category")
    suspend fun maxSortOrder(category: Category): Int

    @Insert
    suspend fun insert(entity: EntryEntity): Long

    @Update
    suspend fun update(entity: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE entries SET pinned = :pinned, updated_at = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE entries SET use_count = use_count + 1, last_used_at = :now WHERE id = :id")
    suspend fun recordUse(id: Long, now: Long)

    @Query("UPDATE entries SET sort_order = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    /** Rewrites sort orders so [orderedIds] appear in that order (0..n-1). */
    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY sort_order, id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Insert
    suspend fun insert(entity: ProfileEntity): Long

    @Update
    suspend fun update(entity: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
