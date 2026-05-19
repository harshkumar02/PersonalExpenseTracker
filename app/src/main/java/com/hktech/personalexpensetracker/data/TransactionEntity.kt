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
    val accountHint: String?,    // Raw card/account number from SMS
    val rawText: String,
    val category: String,
    val confidence: Int = 90,
    val accountId: Long? = null, // Linked account (null = auto-detect or unknown)
    val isTransfer: Boolean = false // True if detected as transfer between own accounts
)