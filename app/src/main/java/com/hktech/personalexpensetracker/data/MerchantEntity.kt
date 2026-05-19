package com.hktech.personalexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val name: String,
    val category: String,
    val aliases: String = "" // comma-separated aliases/typos
)