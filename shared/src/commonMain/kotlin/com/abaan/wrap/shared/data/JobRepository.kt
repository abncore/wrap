package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.NEW_ID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class JobRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
    private val statusUpdater: StatusUpdater,
) {
    private val queries get() = db.jobEntityQueries

    fun observeJobs(): Flow<List<Job>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    fun observeJob(id: Long): Flow<Job?> =
        queries.selectById(id).asFlow().mapToOneOrNull(io).map { it?.toModel() }

    suspend fun getJob(id: Long): Job? = withContext(io) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    /**
     * Inserts a new job (when [Job.id] is [NEW_ID]) or updates it, then brings its status up to
     * date. Returns the job's id.
     */
    suspend fun save(job: Job): Long = withContext(io) {
        queries.transactionWithResult {
            val id = saveInTransaction(job)
            statusUpdater.refresh(id)
            id
        }
    }

    /**
     * Saves a job that has already been worked, together with its shifts
     * (pairs of clock-in / clock-out). Returns the job's id.
     */
    suspend fun saveFinished(job: Job, shifts: List<Pair<Instant, Instant>>): Long = withContext(io) {
        queries.transactionWithResult {
            val status = if (job.status == JobStatus.UPCOMING) JobStatus.DONE_UNPAID else job.status
            val id = saveInTransaction(job.copy(status = status))
            shifts.filter { (start, end) -> end > start }.forEach { (start, end) ->
                db.timeLogEntityQueries.insert(
                    jobId = id,
                    clockIn = start.toEpochMilliseconds(),
                    clockOut = end.toEpochMilliseconds(),
                )
            }
            statusUpdater.refresh(id)
            id
        }
    }

    /** Marks the work as done (or not yet done) and recalculates the status from payments. */
    suspend fun setWorkDone(id: Long, done: Boolean) = withContext(io) {
        db.transaction {
            val status = if (done) JobStatus.DONE_UNPAID else JobStatus.UPCOMING
            queries.updateStatus(status = status.name, id = id)
            statusUpdater.refresh(id)
        }
    }

    /** Deletes the job with its time logs, payments and crew. Spends billed to it are kept, unbilled. */
    suspend fun delete(id: Long) = withContext(io) {
        db.transaction {
            db.timeLogEntityQueries.deleteByJob(id)
            db.paymentEntityQueries.deleteByJob(id)
            db.spendEntityQueries.unlinkJob(id)
            db.crewContactEntityQueries.deleteByJob(id)
            db.chaseLogEntityQueries.deleteByJob(id)
            queries.deleteById(id)
        }
    }

    private fun saveInTransaction(job: Job): Long {
        val title = job.title.trim()
        val role = job.role.trim()
        val location = job.location.trim()
        val notes = job.notes.trim()
        val unitBase = job.unitBase.trim()
        val parking = job.parking.trim()
        return if (job.id == NEW_ID) {
            queries.insert(
                clientId = job.clientId,
                title = title,
                role = role,
                startDate = job.startDate.toDb(),
                endDate = job.endDate.toDb(),
                callTime = job.callTime?.toDbMinutes(),
                location = location,
                rateType = job.rateType.name,
                rate = job.rate,
                dayLengthHours = job.dayLengthHours,
                otMultiplier = job.otMultiplier,
                kitRental = job.kitRental,
                expenses = job.expenses,
                dueDate = job.dueDate?.toDb(),
                status = job.status.name,
                notes = notes,
                unitBase = unitBase,
                parking = parking,
            )
            queries.lastInsertedId().executeAsOne()
        } else {
            queries.update(
                clientId = job.clientId,
                title = title,
                role = role,
                startDate = job.startDate.toDb(),
                endDate = job.endDate.toDb(),
                callTime = job.callTime?.toDbMinutes(),
                location = location,
                rateType = job.rateType.name,
                rate = job.rate,
                dayLengthHours = job.dayLengthHours,
                otMultiplier = job.otMultiplier,
                kitRental = job.kitRental,
                expenses = job.expenses,
                dueDate = job.dueDate?.toDb(),
                status = job.status.name,
                notes = notes,
                unitBase = unitBase,
                parking = parking,
                id = job.id,
            )
            job.id
        }
    }
}
