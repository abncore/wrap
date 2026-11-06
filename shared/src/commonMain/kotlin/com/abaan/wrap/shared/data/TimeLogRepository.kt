package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class TimeLogRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
    private val clock: Clock,
) {
    private val queries get() = db.timeLogEntityQueries

    fun observeAll(): Flow<List<TimeLog>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    fun observeForJob(jobId: Long): Flow<List<TimeLog>> =
        queries.selectByJob(jobId).asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    /** The log the freelancer is currently clocked in on, if any. */
    fun observeOpenLog(): Flow<TimeLog?> =
        queries.selectOpen().asFlow().mapToOneOrNull(io).map { it?.toModel() }

    /**
     * Clocks in on [jobId] now. Any other open log is closed first so there is only ever
     * one running clock. Returns the new log's id.
     */
    suspend fun clockIn(jobId: Long): Long = withContext(io) {
        val now = clock.now()
        queries.transactionWithResult {
            queries.selectOpen().executeAsOneOrNull()?.let { open ->
                queries.closeOpen(clockOut = now.toEpochMilliseconds(), id = open.id)
            }
            queries.insert(jobId = jobId, clockIn = now.toEpochMilliseconds(), clockOut = null)
            queries.lastInsertedId().executeAsOne()
        }
    }

    /** Closes the running log. Returns the job id it belonged to, or null if nothing was running. */
    suspend fun clockOut(): Long? = withContext(io) {
        val now = clock.now()
        queries.transactionWithResult {
            val open = queries.selectOpen().executeAsOneOrNull() ?: return@transactionWithResult null
            queries.closeOpen(clockOut = now.toEpochMilliseconds(), id = open.id)
            open.jobId
        }
    }

    /** Adds a completed log, e.g. for a shift the freelancer forgot to clock. Returns its id. */
    suspend fun addLog(jobId: Long, clockIn: Instant, clockOut: Instant): Long = withContext(io) {
        require(clockOut > clockIn) { "clockOut must be after clockIn" }
        queries.transactionWithResult {
            queries.insert(
                jobId = jobId,
                clockIn = clockIn.toEpochMilliseconds(),
                clockOut = clockOut.toEpochMilliseconds(),
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    suspend fun delete(id: Long) = withContext(io) {
        queries.deleteById(id)
    }
}
