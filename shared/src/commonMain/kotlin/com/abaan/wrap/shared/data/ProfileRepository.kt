package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.Profile
import com.abaan.wrap.shared.money.InvoiceRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The freelancer's own details for invoices, and invoice numbering. */
class ProfileRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.profileEntityQueries

    fun observeProfile(): Flow<Profile> = queries.select().asFlow().mapToOne(io).map { it.toModel() }

    suspend fun getProfile(): Profile = withContext(io) { queries.select().executeAsOne().toModel() }

    suspend fun save(profile: Profile) = withContext(io) {
        queries.update(
            name = profile.name.trim(),
            businessName = profile.businessName.trim(),
            address = profile.address.trim(),
            phone = profile.phone.trim(),
            email = profile.email.trim(),
            trn = profile.trn.trim(),
            bankDetails = profile.bankDetails.trim(),
            vatRegistered = if (profile.vatRegistered) 1L else 0L,
            invoicePrefix = profile.invoicePrefix.trim(),
            nextInvoiceNumber = profile.nextInvoiceNumber.coerceAtLeast(1),
        )
    }

    /**
     * The invoice number for [jobId]. A job keeps its number once it has one, so sending the invoice
     * again doesn't use up a new number; otherwise it takes the next one.
     */
    suspend fun invoiceNumberFor(jobId: Long): String = withContext(io) {
        db.transactionWithResult {
            val job = db.jobEntityQueries.selectById(jobId).executeAsOne()
            job.invoiceNumber ?: run {
                val profile = queries.select().executeAsOne()
                val number = InvoiceRules.formatNumber(profile.invoicePrefix, profile.nextInvoiceNumber)
                db.jobEntityQueries.setInvoiceNumber(number, jobId)
                queries.takeInvoiceNumber()
                number
            }
        }
    }
}
