package com.hktech.personalexpensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY ts DESC")
    fun all(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET category = :category, confidence = 100 WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)
}
