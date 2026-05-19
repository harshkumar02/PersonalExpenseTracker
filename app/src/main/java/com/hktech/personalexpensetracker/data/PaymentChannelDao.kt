package com.hktech.personalexpensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentChannelDao {
    @Query("SELECT * FROM payment_channels ORDER BY displayName ASC")
    fun getAll(): Flow<List<PaymentChannelEntity>>

    @Query("SELECT * FROM payment_channels ORDER BY displayName ASC")
    suspend fun getAllList(): List<PaymentChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: PaymentChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<PaymentChannelEntity>)

    @Delete
    suspend fun delete(channel: PaymentChannelEntity)

    @Query("DELETE FROM payment_channels WHERE code = :code")
    suspend fun deleteByCode(code: String)
}