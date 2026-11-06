package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.BudgetStatus
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.money.BudgetRules
import com.abaan.wrap.shared.money.Insights
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Result of saving a spend: its id, the alert to raise (if any) and the budget afterwards. */
data class BudgetAlert(
    val spendId: Long,
    val level: BudgetLevel?,
    val status: BudgetStatus?,
)

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

    /** Total spent in the calendar month containing [date]. */
    suspend fun spentInMonth(date: LocalDate): Long = withContext(io) {
        val first = Insights.firstOfMonth(date)
        val last = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        queries.sumBetween(first.toDb(), last.toDb()).executeAsOne()
    }

    /**
     * Saves [spend] and returns the budget alert it triggers for the current month, if any: NEAR when
     * this month's spending first reaches 80% of [monthlyBudget], OVER when it first goes past it.
     */
    suspend fun saveAndCheckBudget(spend: Spend, monthlyBudget: Long, today: LocalDate): BudgetAlert {
        val before = spentInMonth(today)
        val id = save(spend)
        val after = spentInMonth(today)
        return BudgetAlert(id, BudgetRules.alertFor(before, after, monthlyBudget), BudgetRules.status(monthlyBudget, after, today))
    }
}
