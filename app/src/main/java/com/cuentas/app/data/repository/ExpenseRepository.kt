package com.cuentas.app.data.repository

import com.cuentas.app.data.db.CategorySummary
import com.cuentas.app.data.db.ExpenseDao
import com.cuentas.app.data.db.StoreSummary
import com.cuentas.app.data.model.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    fun getRecentExpenses(): Flow<List<Expense>> = dao.getRecentExpenses()

    fun getFilteredExpenses(
        startTime: Long,
        endTime: Long,
        startHour: Int? = null,
        endHour: Int? = null,
        category: String? = null,
        storeQuery: String? = null
    ): Flow<List<Expense>> = dao.getFilteredExpenses(
        startTime, endTime, startHour, endHour, category, storeQuery
    )

    fun getTotalSpent(): Flow<Double?> = dao.getTotalSpent()

    fun getTotalByDateRange(startTime: Long, endTime: Long): Flow<Double?> =
        dao.getTotalByDateRange(startTime, endTime)

    fun getCategorySummary(startTime: Long, endTime: Long): Flow<List<CategorySummary>> =
        dao.getCategorySummary(startTime, endTime)

    fun getTopStores(startTime: Long, endTime: Long): Flow<List<StoreSummary>> =
        dao.getTopStores(startTime, endTime)

    fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()

    fun getAllStores(): Flow<List<String>> = dao.getAllStores()

    suspend fun insertExpense(expense: Expense): Long = dao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = dao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    /** Rango desde inicio del día de hoy hasta ahora */
    fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    /** Rango para el mes actual */
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    /** Últimos N días */
    fun getLastNDaysRange(days: Int): Pair<Long, Long> {
        val end = System.currentTimeMillis()
        val start = end - (days * 24 * 60 * 60 * 1000L)
        return Pair(start, end)
    }
}
