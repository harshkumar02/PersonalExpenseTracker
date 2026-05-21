package com.hktech.personalexpensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY bankName ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY bankName ASC")
    suspend fun getAllList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE cardSuffix = :suffix LIMIT 1")
    suspend fun getByCardSuffix(suffix: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("UPDATE accounts SET currentBalance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Double)
}