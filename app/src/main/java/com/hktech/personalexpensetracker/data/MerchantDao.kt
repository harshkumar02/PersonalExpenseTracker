package com.hktech.personalexpensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants ORDER BY name ASC")
    fun getAll(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants ORDER BY name ASC")
    suspend fun getAllList(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE category = :category")
    fun getByCategory(category: String): Flow<List<MerchantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchant: MerchantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(merchants: List<MerchantEntity>)

    @Delete
    suspend fun delete(merchant: MerchantEntity)

    @Query("DELETE FROM merchants WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM merchants")
    suspend fun deleteAll()

    @Query("SELECT * FROM merchants WHERE :keyword LIKE '%' || LOWER(name) || '%' OR :keyword LIKE '%' || LOWER(aliases) || '%' LIMIT 1")
    suspend fun findByKeyword(keyword: String): MerchantEntity?
}