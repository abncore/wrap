package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.money.JobStatusRules
import com.abaan.wrap.shared.money.MoneyRules

/**
 * Keeps each job's stored status in step with its time logs and payments.
 * Must be called inside a database transaction after anything that affects a job's money.
 */
class StatusUpdater(
    private val db: WrapDatabase,
    private val today: Today,
) {
    internal fun refresh(jobId: Long) {
        val job = db.jobEntityQueries.selectById(jobId).executeAsOneOrNull()?.toModel() ?: return
        val logs = db.timeLogEntityQueries.selectByJob(jobId).executeAsList().map { it.toModel() }
        val payments = db.paymentEntityQueries.selectByJob(jobId).executeAsList().map { it.toModel() }
        val money = MoneyRules.breakdown(job, logs, payments, today.timeZone)
        val workDone = JobStatusRules.isWorkDone(job.status, job.endDate, logs, today.date(), today.timeZone)
        val status = JobStatusRules.storedStatus(workDone, money.total, money.paid)
        if (status != job.status) {
            db.jobEntityQueries.updateStatus(status = status.name, id = jobId)
        }
    }
}
