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
