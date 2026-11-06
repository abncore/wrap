package com.abaan.wrap.shared.model

import com.abaan.wrap.shared.money.MoneyBreakdown
import kotlinx.datetime.LocalDate

/** A job with everything needed to display it, computed in shared code. */
data class JobSummary(
    val job: Job,
    val client: Client?,
    val money: MoneyBreakdown,
    /** Status to display; OVERDUE when money is owed past the due date. */
    val status: JobStatus,
    val workDone: Boolean,
    /** True when the dates or time logs alone make the job done, so it can't be marked not done. */
    val workDoneAutomatically: Boolean,
    val timeLogs: List<TimeLog>,
    val payments: List<Payment>,
    /** True when today falls within the job's dates. */
    val isOnToday: Boolean,
    /** The running log for this job, if the freelancer is clocked in on it. */
    val openLog: TimeLog?,
) {
    val isOverdue: Boolean get() = status == JobStatus.OVERDUE
    val isPaid: Boolean get() = status == JobStatus.PAID
    /** The freelancer can clock in: it's a job day and the work isn't finished, or a clock is running. */
    val canClock: Boolean get() = (isOnToday && !workDone) || openLog != null
    val isUnpaid: Boolean
        get() = status == JobStatus.DONE_UNPAID || status == JobStatus.PART_PAID || status == JobStatus.OVERDUE
    val dateRange: ClosedRange<LocalDate> get() = job.startDate..job.endDate
}

/** Numbers for the Home screen. Amounts are minor units. */
data class Dashboard(
    val currency: String,
    val owedTotal: Long,
    /** Finished jobs that still have money owed. */
    val unpaidCount: Int,
    val receivedThisMonth: Long,
    /** Every payment ever received. */
    val earnedLifetime: Long,
    val spentThisMonth: Long,
    val overdueCount: Int,
    val overdueTotal: Long,
    /** Today's unfinished (or clocked-in) job, if any, so the freelancer can clock in or out. */
    val todayJob: JobSummary?,
    /** The next job that hasn't started yet (or today's, if it isn't done). */
    val nextJob: JobSummary?,
    val openLog: TimeLog?,
    val hasAnyJobs: Boolean,
)

/** A client with what they owe across all their jobs. */
data class ClientSummary(
    val client: Client,
    val owed: Long,
    val overdue: Boolean,
    val jobCount: Int,
)
