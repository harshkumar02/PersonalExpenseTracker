package com.hktech.personalexpensetracker.backup

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("appVersion") val appVersion: String = "1.0",
    @SerializedName("transactions") val transactions: List<TransactionBackup> = emptyList(),
    @SerializedName("categories") val categories: List<CategoryBackup> = emptyList(),
    @SerializedName("merchants") val merchants: List<MerchantBackup> = emptyList(),
    @SerializedName("accounts") val accounts: List<AccountBackup> = emptyList(),
    @SerializedName("paymentChannels") val paymentChannels: List<PaymentChannelBackup> = emptyList()
)

data class TransactionBackup(
    @SerializedName("id") val id: Long,
    @SerializedName("ts") val ts: Long,
    @SerializedName("source") val source: String,
    @SerializedName("channel") val channel: String?,
    @SerializedName("direction") val direction: String,
    @SerializedName("merchant") val merchant: String?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("accountHint") val accountHint: String?,
    @SerializedName("rawText") val rawText: String,
    @SerializedName("category") val category: String,
    @SerializedName("confidence") val confidence: Int,
    @SerializedName("accountId") val accountId: Long?,
    @SerializedName("isTransfer") val isTransfer: Boolean
)

data class CategoryBackup(
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String
)

data class MerchantBackup(
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("aliases") val aliases: String
)

data class AccountBackup(
    @SerializedName("id") val id: Long,
    @SerializedName("bankName") val bankName: String,
    @SerializedName("cardSuffix") val cardSuffix: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("accountType") val accountType: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("balance") val balance: Double?
)

data class PaymentChannelBackup(
    @SerializedName("code") val code: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("keywords") val keywords: String
)