package app.tote.core.data.repo

import app.tote.core.data.db.LearnedDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** One remembered pick: on [source], for the field described by [signature], the user chose [entryId]. */
data class LearnedMapping(
    val source: String,
    val signature: String,
    val entryId: Long,
    val hits: Int,
    val lastUsedAt: Long,
)

/** Local learning (Plan §6.3): what the user picked for which field, so it ranks first next time. */
interface LearningRepository {
    val mappings: Flow<List<LearnedMapping>>
    suspend fun record(source: String, signature: String, entryId: Long)
    suspend fun forget(source: String, signature: String)
    suspend fun clear()
}

@Singleton
class RoomLearningRepository @Inject constructor(
    private val dao: LearnedDao,
    private val clock: Clock,
) : LearningRepository {

    override val mappings: Flow<List<LearnedMapping>> = dao.observeAll().map { rows ->
        rows.map { LearnedMapping(it.source, it.signature, it.entryId, it.hits, it.lastUsedAt) }
    }

    override suspend fun record(source: String, signature: String, entryId: Long) =
        dao.record(source, signature, entryId, clock.now())

    override suspend fun forget(source: String, signature: String) = dao.forget(source, signature)

    override suspend fun clear() = dao.clear()
}
