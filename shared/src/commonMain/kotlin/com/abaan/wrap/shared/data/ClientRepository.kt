package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.NEW_ID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ClientRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.clientEntityQueries

    fun observeClients(): Flow<List<Client>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    suspend fun getClient(id: Long): Client? = withContext(io) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    /** Inserts a new client (when [Client.id] is [NEW_ID]) or updates it. Returns the client's id. */
    suspend fun save(client: Client): Long = withContext(io) {
        queries.transactionWithResult {
            if (client.id == NEW_ID) {
                queries.insert(
                    name = client.name.trim(),
                    company = client.company.trim(),
                    phone = client.phone.trim(),
                    email = client.email.trim(),
                    paymentTermsDays = client.paymentTermsDays?.toLong(),
                    defaultRate = client.defaultRate,
                    notes = client.notes.trim(),
                )
                queries.lastInsertedId().executeAsOne()
            } else {
                queries.update(
                    name = client.name.trim(),
                    company = client.company.trim(),
                    phone = client.phone.trim(),
                    email = client.email.trim(),
                    paymentTermsDays = client.paymentTermsDays?.toLong(),
                    defaultRate = client.defaultRate,
                    notes = client.notes.trim(),
                    id = client.id,
                )
                client.id
            }
        }
    }

    /** Deletes the client if it has no jobs. Returns false (and deletes nothing) otherwise. */
    suspend fun delete(id: Long): Boolean = withContext(io) {
        queries.transactionWithResult {
            val jobCount = db.jobEntityQueries.countByClient(id).executeAsOne()
            if (jobCount > 0) {
                false
            } else {
                queries.deleteById(id)
                true
            }
        }
    }
}
