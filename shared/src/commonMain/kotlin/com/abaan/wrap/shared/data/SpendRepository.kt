package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.Spend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SpendRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.spendEntityQueries

    fun observeAll(): Flow<List<Spend>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    suspend fun getSpend(id: Long): Spend? = withContext(io) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    /** Inserts a new spend (when [Spend.id] is [NEW_ID]) or updates it. Returns its id. */
    suspend fun save(spend: Spend): Long = withContext(io) {
        require(spend.amount > 0) { "Spend amount must be positive" }
        queries.transactionWithResult {
            if (spend.id == NEW_ID) {
                queries.insert(
                    amount = spend.amount,
                    category = spend.category.name,
                    date = spend.date.toDb(),
                    note = spend.note.trim(),
                )
                queries.lastInsertedId().executeAsOne()
            } else {
                queries.update(
                    amount = spend.amount,
                    category = spend.category.name,
                    date = spend.date.toDb(),
                    note = spend.note.trim(),
                    id = spend.id,
                )
                spend.id
            }
        }
    }

    suspend fun delete(id: Long) = withContext(io) {
        queries.deleteById(id)
    }
}
