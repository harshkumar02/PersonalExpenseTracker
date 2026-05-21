package com.hktech.personalexpensetracker.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hktech.personalexpensetracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FOLDER = "ExpenseTrackerBackups"
        private const val MIME_TYPE_JSON = "application/json"
    }

    private fun getDownloadsDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDir = File(downloadsDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    private fun saveToDownloadsViaMediaStore(json: String, fileName: String): File? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE_JSON)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + BACKUP_FOLDER)
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { backupUri ->
                    context.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Log.d(TAG, "Backup saved to Downloads via MediaStore")
                }
            }

            // Always save to accessible location too
            val backupDir = getDownloadsDir()
            val backupFile = File(backupDir, fileName)
            backupFile.writeText(json)
            Log.d(TAG, "Backup saved to: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup", e)
            null
        }
    }

    suspend fun createBackup(data: BackupData): File? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Creating backup with ${data.transactions.size} transactions")

        val json = gson.toJson(data)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "backup_$timestamp.json"

        saveToDownloadsViaMediaStore(json, fileName)
    }

    suspend fun createFullBackup(db: AppDatabase, appVersion: String): File? = withContext(Dispatchers.IO) {
        val transactions = db.txnDao().all().first()
        val categories = db.categoryDao().getAllList()
        val merchants = db.merchantDao().getAllList()
        val accounts = db.accountDao().getAllList()
        val channels = db.paymentChannelDao().getAllList()

        val backupData = BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            transactions = transactions.map { t ->
                TransactionBackup(
                    id = t.id,
                    ts = t.ts,
                    source = t.source,
                    channel = t.channel,
                    direction = t.direction,
                    merchant = t.merchant,
                    amount = t.amount,
                    currency = t.currency,
                    accountHint = t.accountHint,
                    rawText = t.rawText,
                    category = t.category,
                    confidence = t.confidence,
                    accountId = t.accountId,
                    isTransfer = t.isTransfer
                )
            },
            categories = categories.map { c ->
                CategoryBackup(name = c.name, color = c.color)
            },
            merchants = merchants.map { m ->
                MerchantBackup(name = m.name, category = m.category, aliases = m.aliases)
            },
            accounts = accounts.map { a ->
                AccountBackup(
                    id = a.id,
                    bankName = a.bankName,
                    cardSuffix = a.cardSuffix,
                    nickname = a.nickname,
                    accountType = a.accountType,
                    isActive = a.isActive,
                    balance = a.currentBalance
                )
            },
            paymentChannels = channels.map { ch ->
                PaymentChannelBackup(code = ch.code, displayName = ch.displayName, keywords = ch.keywords)
            }
        )

        createBackup(backupData)
    }

    suspend fun restoreBackup(file: File, db: AppDatabase, onProgress: (String) -> Unit = {}): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val json = file.readText()
            val backupData = gson.fromJson(json, BackupData::class.java)

            Log.d(TAG, "Restoring backup version ${backupData.version}, timestamp ${backupData.timestamp}")

            onProgress("Clearing existing data...")

            db.txnDao().deleteAll()
            db.categoryDao().deleteAll()
            db.merchantDao().deleteAll()
            db.accountDao().deleteAll()
            db.paymentChannelDao().deleteAll()

            onProgress("Restoring ${backupData.categories.size} categories...")
            backupData.categories.forEach { cat ->
                db.categoryDao().insert(CategoryEntity(name = cat.name, color = cat.color))
            }

            onProgress("Restoring ${backupData.merchants.size} merchants...")
            backupData.merchants.forEach { merchant ->
                db.merchantDao().insert(MerchantEntity(name = merchant.name, category = merchant.category, aliases = merchant.aliases))
            }

            onProgress("Restoring ${backupData.paymentChannels.size} payment channels...")
            backupData.paymentChannels.forEach { channel ->
                db.paymentChannelDao().insert(PaymentChannelEntity(code = channel.code, displayName = channel.displayName, keywords = channel.keywords))
            }

            onProgress("Restoring ${backupData.accounts.size} accounts...")
            backupData.accounts.forEach { account ->
                db.accountDao().insert(AccountEntity(
                    id = 0,
                    bankName = account.bankName,
                    cardSuffix = account.cardSuffix,
                    nickname = account.nickname,
                    accountType = account.accountType,
                    isActive = account.isActive,
                    currentBalance = account.balance
                ))
            }

            onProgress("Restoring ${backupData.transactions.size} transactions...")
            backupData.transactions.forEach { txn ->
                db.txnDao().insert(TransactionEntity(
                    id = 0,
                    ts = txn.ts,
                    source = txn.source,
                    channel = txn.channel,
                    direction = txn.direction,
                    merchant = txn.merchant,
                    amount = txn.amount,
                    currency = txn.currency,
                    accountHint = txn.accountHint,
                    rawText = txn.rawText,
                    category = txn.category,
                    confidence = txn.confidence,
                    accountId = txn.accountId,
                    isTransfer = txn.isTransfer
                ))
            }

            onProgress("Restore complete!")

            RestoreResult(
                success = true,
                transactionsRestored = backupData.transactions.size,
                categoriesRestored = backupData.categories.size,
                merchantsRestored = backupData.merchants.size,
                accountsRestored = backupData.accounts.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            RestoreResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    suspend fun restoreFromJson(json: String, db: AppDatabase, onProgress: (String) -> Unit = {}): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val backupData = gson.fromJson(json, BackupData::class.java)
            val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
            tempFile.writeText(json)
            val result = restoreBackup(tempFile, db, onProgress)
            tempFile.delete()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Restore from JSON failed", e)
            RestoreResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDir = getDownloadsDir()
            val latestBackup = backupDir.listFiles()?.maxByOrNull { it.lastModified() }

            if (latestBackup == null) {
                Log.e(TAG, "No backup file found")
                return@withContext false
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                latestBackup.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(TAG, "Exported backup to URI")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }

    suspend fun importFromUri(uri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = inputStream.bufferedReader().readText()
                gson.fromJson(json, BackupData::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            null
        }
    }

    fun getLatestBackupFile(): File? {
        val backupDir = getDownloadsDir()
        return backupDir.listFiles()?.maxByOrNull { it.lastModified() }
    }

    fun getBackupPath(): String = getDownloadsDir().absolutePath

    fun getAllBackupFiles(): List<File> {
        val backupDir = getDownloadsDir()
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun cleanOldBackups(keepCount: Int = 10) {
        val files = getAllBackupFiles()
        if (files.size > keepCount) {
            files.drop(keepCount).forEach { it.delete() }
            Log.d(TAG, "Cleaned ${files.size - keepCount} old backup files")
        }
    }
}

data class RestoreResult(
    val success: Boolean,
    val error: String? = null,
    val transactionsRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val merchantsRestored: Int = 0,
    val accountsRestored: Int = 0
)