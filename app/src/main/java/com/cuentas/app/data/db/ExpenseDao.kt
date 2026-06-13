package com.cuentas.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cuentas.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

data class CategorySummary(val category: String, val total: Double)
data class StoreSummary(val storeName: String, val total: Double)
data class DailySummary(val dayLabel: String, val total: Double)

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 10")
    fun getRecentExpenses(): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses 
        WHERE timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses
        WHERE timestamp BETWEEN :startTime AND :endTime
        AND ((:hour1 IS NULL) OR (((timestamp / 1000) % 86400) / 3600 BETWEEN :hour1 AND :hour2))
        AND ((:category IS NULL OR :category = '') OR category = :category)
        AND ((:storeQuery IS NULL OR :storeQuery = '') OR storeName LIKE '%' || :storeQuery || '%')
        ORDER BY timestamp DESC
    """)
    fun getFilteredExpenses(
        startTime: Long,
        endTime: Long,
        hour1: Int?,
        hour2: Int?,
        category: String?,
        storeQuery: String?
    ): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getTotalByDateRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalSpent(): Flow<Double?>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM expenses 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getCategorySummary(startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Query("""
        SELECT storeName, SUM(amount) as total 
        FROM expenses 
        WHERE storeName != '' AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY storeName 
        ORDER BY total DESC 
        LIMIT 5
    """)
    fun getTopStores(startTime: Long, endTime: Long): Flow<List<StoreSummary>>

    @Query("SELECT DISTINCT category FROM expenses ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT storeName FROM expenses WHERE storeName != '' ORDER BY storeName")
    fun getAllStores(): Flow<List<String>>

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}
