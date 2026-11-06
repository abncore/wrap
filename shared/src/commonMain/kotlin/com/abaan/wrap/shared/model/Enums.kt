package com.abaan.wrap.shared.model

/** How a job's base pay is calculated. */
enum class RateType {
    DAY,
    HALF_DAY,
    HOUR,
    FIXED,
}

/** How a payment was received. */
enum class PaymentMethod {
    CASH,
    BANK,
    CHEQUE,
}

/**
 * Stored lifecycle status of a job. [OVERDUE] is never stored; it is derived when
 * money is still owed after the due date.
 */
enum class JobStatus {
    UPCOMING,
    DONE_UNPAID,
    PART_PAID,
    PAID,
    OVERDUE,
}

/** What money was spent on. */
enum class SpendCategory {
    FUEL,
    TRANSPORT,
    FOOD,
    KIT,
    PHONE,
    SOFTWARE,
    PERMITS,
    OTHER,
}

/** Time span for the spending-by-category breakdown. */
enum class InsightPeriod {
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME,
}
