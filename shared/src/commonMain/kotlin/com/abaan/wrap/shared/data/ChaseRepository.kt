package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.chase.ChaseRules
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.ChaseKind
import com.abaan.wrap.shared.model.ChaseLog
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** When payments were chased, and which overdue jobs are due a reminder to chase. */
class ChaseRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
    private val clock: Clock,
    private val today: Today,
) {
    private val queries get() = db.chaseLogEntityQueries

    /** Times the client was chased for [jobId], newest first. */
    fun observeChases(jobId: Long): Flow<List<ChaseLog>> =
        queries.selectByJob(jobId).asFlow().mapToList(io).map { rows ->
            rows.map { it.toModel() }.filter { it.kind == ChaseKind.CHASED }
        }

    suspend fun recordChase(jobId: Long) = record(jobId, ChaseKind.CHASED)

    suspend fun recordNudge(jobId: Long) = record(jobId, ChaseKind.NUDGED)

    /** The overdue jobs among [summaries] that should get a "time to chase" reminder today. */
    suspend fun jobsToNudge(summaries: List<JobSummary>): List<JobSummary> = withContext(io) {
        val date = today.date()
        val byJob = queries.selectAll().executeAsList().map { it.toModel() }.groupBy { it.jobId }
        summaries.filter { summary ->
            val logs = byJob[summary.job.id].orEmpty()
            fun last(kind: ChaseKind) = logs.firstOrNull { it.kind == kind }?.at?.toLocalDateTime(today.timeZone)?.date
            ChaseRules.needsNudge(summary, lastChased = last(ChaseKind.CHASED), lastNudged = last(ChaseKind.NUDGED), today = date)
        }
    }

    private suspend fun record(jobId: Long, kind: ChaseKind) = withContext(io) {
        queries.insert(jobId = jobId, at = clock.now().toEpochMilliseconds(), kind = kind.name)
    }
}
