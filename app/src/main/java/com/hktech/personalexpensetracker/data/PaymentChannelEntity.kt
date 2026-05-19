package com.hktech.personalexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_channels")
data class PaymentChannelEntity(
    @PrimaryKey val code: String,  // e.g., "UPI", "CARD"
    val displayName: String,        // e.g., "UPI", "Debit Card"
    val keywords: String = ""       // comma-separated keywords to detect this channel
)