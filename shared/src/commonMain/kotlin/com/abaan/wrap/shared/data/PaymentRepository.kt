package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.Payment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PaymentRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.paymentEntityQueries

    fun observeAll(): Flow<List<Payment>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    fun observeForJob(jobId: Long): Flow<List<Payment>> =
        queries.selectByJob(jobId).asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    /** Inserts a new payment (when [Payment.id] is [NEW_ID]) or updates it. Returns its id. */
    suspend fun save(payment: Payment): Long = withContext(io) {
        require(payment.amount > 0) { "Payment amount must be positive" }
        queries.transactionWithResult {
            if (payment.id == NEW_ID) {
                queries.insert(
                    jobId = payment.jobId,
                    amount = payment.amount,
                    dateReceived = payment.dateReceived.toDb(),
                    method = payment.method.name,
                    note = payment.note.trim(),
                )
                queries.lastInsertedId().executeAsOne()
            } else {
                queries.update(
                    amount = payment.amount,
                    dateReceived = payment.dateReceived.toDb(),
                    method = payment.method.name,
                    note = payment.note.trim(),
                    id = payment.id,
                )
                payment.id
            }
        }
    }

    suspend fun delete(id: Long) = withContext(io) {
        queries.deleteById(id)
    }
}
