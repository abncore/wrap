package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.NEW_ID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JobRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.jobEntityQueries

    fun observeJobs(): Flow<List<Job>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    fun observeJob(id: Long): Flow<Job?> =
        queries.selectById(id).asFlow().mapToOneOrNull(io).map { it?.toModel() }

    suspend fun getJob(id: Long): Job? = withContext(io) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    /** Inserts a new job (when [Job.id] is [NEW_ID]) or updates it. Returns the job's id. */
    suspend fun save(job: Job): Long = withContext(io) {
        queries.transactionWithResult { saveInTransaction(job) }
    }

    /** Deletes the job together with its time logs and payments. */
    suspend fun delete(id: Long) = withContext(io) {
        db.transaction {
            db.timeLogEntityQueries.deleteByJob(id)
            db.paymentEntityQueries.deleteByJob(id)
            queries.deleteById(id)
        }
    }

    internal fun saveInTransaction(job: Job): Long {
        val title = job.title.trim()
        val role = job.role.trim()
        val location = job.location.trim()
        val notes = job.notes.trim()
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
                id = job.id,
            )
            job.id
        }
    }
}
