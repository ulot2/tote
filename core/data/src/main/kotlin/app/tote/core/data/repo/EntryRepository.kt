package app.tote.core.data.repo

import app.tote.core.data.db.EntryDao
import app.tote.core.data.db.toEntity
import app.tote.core.data.db.toModel
import app.tote.core.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface EntryRepository {
    /** All entries ordered by category then manual sort order. */
    val entries: Flow<List<Entry>>
    fun observe(id: Long): Flow<Entry?>
    suspend fun get(id: Long): Entry?

    /** Inserts when [Entry.id] is 0, otherwise updates. Returns the id. */
    suspend fun save(entry: Entry): Long
    suspend fun delete(id: Long)

    /** Re-inserts a previously deleted entry (undo), keeping its id and stats. */
    suspend fun restore(entry: Entry)
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun recordUse(id: Long)

    /** Persists a new manual order for entries within one category. */
    suspend fun reorder(orderedIds: List<Long>)
}

fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

@Singleton
class RoomEntryRepository @Inject constructor(
    private val dao: EntryDao,
    private val clock: Clock,
) : EntryRepository {

    override val entries: Flow<List<Entry>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observe(id: Long): Flow<Entry?> = dao.observe(id).map { it?.toModel() }

    override suspend fun get(id: Long): Entry? = dao.get(id)?.toModel()

    override suspend fun save(entry: Entry): Long {
        val now = clock.now()
        val existing = if (entry.id != 0L) dao.get(entry.id) else null
        return if (existing == null) {
            val order = dao.maxSortOrder(entry.category) + 1
            dao.insert(entry.copy(id = 0, sortOrder = order, createdAt = now, updatedAt = now).toEntity())
        } else {
            // Moving to another category puts the entry at the end of that section.
            val order = if (existing.category != entry.category) dao.maxSortOrder(entry.category) + 1 else existing.sortOrder
            dao.update(
                entry.copy(
                    sortOrder = order,
                    useCount = existing.useCount,
                    lastUsedAt = existing.lastUsedAt,
                    createdAt = existing.createdAt,
                    updatedAt = now,
                ).toEntity(),
            )
            entry.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun restore(entry: Entry) {
        if (dao.get(entry.id) == null) dao.insert(entry.toEntity())
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned, clock.now())

    override suspend fun recordUse(id: Long) = dao.recordUse(id, clock.now())

    override suspend fun reorder(orderedIds: List<Long>) = dao.reorder(orderedIds)
}
