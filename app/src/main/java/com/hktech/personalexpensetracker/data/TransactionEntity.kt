package com.hktech.personalexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,
    val source: String,          // "SMS"
    val channel: String?,        // "UPI"/"CARD"/...
    val direction: String,       // "DEBIT"/"CREDIT"
    val merchant: String?,
    val amount: Double,
    val currency: String = "INR",
    val accountHint: String?,
    val rawText: String,
    val category: String,
    val confidence: Int = 90
)
