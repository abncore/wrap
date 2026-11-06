package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Status flow: UPCOMING -> DONE_UNPAID -> PART_PAID -> PAID.
 *
 * A job counts as worked once it has been marked done (any stored status past UPCOMING),
 * its end date has passed, or the freelancer has clocked out on (or after) its last day.
 * OVERDUE is never stored: it is derived when money is still owed after the due date.
 */
object JobStatusRules {

    fun isWorkDone(
        storedStatus: JobStatus,
        endDate: LocalDate,
        timeLogs: List<TimeLog>,
        today: LocalDate,
        timeZone: TimeZone,
    ): Boolean =
        storedStatus != JobStatus.UPCOMING ||
            today > endDate ||
            timeLogs.any { log -> log.clockOut != null && log.clockOut.toLocalDateTime(timeZone).date >= endDate }

    /** The status to store, given whether the work is done and how much has been paid. */
    fun storedStatus(workDone: Boolean, total: Long, paid: Long): JobStatus = when {
        !workDone -> JobStatus.UPCOMING
        paid >= total -> JobStatus.PAID
        paid <= 0 -> JobStatus.DONE_UNPAID
        else -> JobStatus.PART_PAID
    }

    fun isOverdue(status: JobStatus, owed: Long, dueDate: LocalDate?, today: LocalDate): Boolean =
        dueDate != null &&
            owed > 0 &&
            today > dueDate &&
            (status == JobStatus.DONE_UNPAID || status == JobStatus.PART_PAID)

    /** The status to show: the stored status, or OVERDUE when it applies. */
    fun displayStatus(status: JobStatus, owed: Long, dueDate: LocalDate?, today: LocalDate): JobStatus =
        if (isOverdue(status, owed, dueDate, today)) JobStatus.OVERDUE else status

    /** Default due date: the job's last day plus the payment terms. */
    fun defaultDueDate(endDate: LocalDate, paymentTermsDays: Int): LocalDate =
        endDate.plus(DatePeriod(days = paymentTermsDays.coerceAtLeast(0)))
}
