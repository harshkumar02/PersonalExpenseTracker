package com.hktech.personalexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bankName: String,
    val cardSuffix: String,           // Last 4 digits of card/account (e.g., "2007")
    val nickname: String = "",        // User-friendly name (e.g., "ICICI Credit")
    val accountType: String = "CARD",  // "CARD", "ACCOUNT", "UPI", "WALLET"
    val isActive: Boolean = true,
    val currentBalance: Double? = null // Balance extracted from latest SMS
)
