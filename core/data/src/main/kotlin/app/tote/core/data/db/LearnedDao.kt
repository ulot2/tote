package app.tote.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedDao {

    @Query("SELECT * FROM learned_mappings ORDER BY last_used_at DESC")
    fun observeAll(): Flow<List<LearnedMappingEntity>>

    @Query("INSERT OR IGNORE INTO learned_mappings (source, signature, entry_id, hits, last_used_at) VALUES (:source, :signature, :entryId, 0, :now)")
    suspend fun insertIfMissing(source: String, signature: String, entryId: Long, now: Long)

    @Query("UPDATE learned_mappings SET hits = hits + 1, last_used_at = :now WHERE source = :source AND signature = :signature AND entry_id = :entryId")
    suspend fun bump(source: String, signature: String, entryId: Long, now: Long)

    /** Counts one pick. Upsert by hand: Room's @Upsert would reset the hit count. */
    @Transaction
    suspend fun record(source: String, signature: String, entryId: Long, now: Long) {
        insertIfMissing(source, signature, entryId, now)
        bump(source, signature, entryId, now)
    }

    @Query("DELETE FROM learned_mappings WHERE source = :source AND signature = :signature")
    suspend fun forget(source: String, signature: String)

    @Query("DELETE FROM learned_mappings")
    suspend fun clear()
}
