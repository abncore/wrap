package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.RecurringSpend
import com.abaan.wrap.shared.money.RecurringRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/** Monthly costs that add a spend by themselves on the same day each month. */
class RecurringSpendRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.recurringSpendEntityQueries

    fun observeAll(): Flow<List<RecurringSpend>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    /**
     * Makes the saved spend [spendId] repeat every month on its day of the month, or stops it
     * repeating. When it already repeats, the monthly cost takes the spend's amount, category and note.
     * Any months already due up to [today] are added straight away.
     */
    suspend fun setRepeating(spendId: Long, repeat: Boolean, today: LocalDate) {
        withContext(io) {
            db.transaction {
                val spend = db.spendEntityQueries.selectById(spendId).executeAsOneOrNull()?.toModel()
                    ?: return@transaction
                val ruleId = spend.recurringId?.takeIf { queries.selectById(it).executeAsOneOrNull() != null }
                when {
                    repeat && ruleId != null -> queries.update(
                        amount = spend.amount,
                        category = spend.category.name,
                        note = spend.note,
                        id = ruleId,
                    )
                    repeat -> {
                        queries.insert(
                            amount = spend.amount,
                            category = spend.category.name,
                            note = spend.note,
                            dayOfMonth = spend.date.day.toLong(),
                            nextDate = RecurringRules.nextAfter(spend.date, spend.date.day).toDb(),
                        )
                        db.spendEntityQueries.setRecurring(queries.lastInsertedId().executeAsOne(), spendId)
                    }
                    ruleId != null -> stopInTransaction(ruleId)
                }
            }
        }
        addDue(today)
    }

    /** Stops a monthly cost. Spends it already added are kept. */
    suspend fun stop(id: Long) = withContext(io) {
        db.transaction { stopInTransaction(id) }
    }

    /** Adds a spend for every monthly cost that has come due up to [today]. Returns how many were added. */
    suspend fun addDue(today: LocalDate): Int = withContext(io) {
        db.transactionWithResult {
            queries.selectDue(today.toDb()).executeAsList().sumOf { rule ->
                val dayOfMonth = rule.dayOfMonth.toInt()
                val dates = RecurringRules.dueDates(rule.nextDate.toLocalDate(), dayOfMonth, today)
                dates.forEach { date ->
                    db.spendEntityQueries.insert(
                        amount = rule.amount,
                        category = rule.category,
                        date = date.toDb(),
                        note = rule.note,
                        recurringId = rule.id,
                    )
                }
                dates.lastOrNull()?.let { last ->
                    queries.updateNextDate(RecurringRules.nextAfter(last, dayOfMonth).toDb(), rule.id)
                }
                dates.size
            }
        }
    }

    private fun stopInTransaction(id: Long) {
        queries.deleteById(id)
        db.spendEntityQueries.clearRecurring(id)
    }
}
