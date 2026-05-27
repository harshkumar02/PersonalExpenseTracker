package com.hktech.personalexpensetracker.ingest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.hktech.personalexpensetracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SMS BroadcastReceiver that processes incoming transaction SMS messages.
 *
 * Key design decisions:
 * - Sequential processing to maintain insertion order
 * - AtomicBoolean ensures only one batch processes at a time
 * - Account refresh happens once per batch before processing
 */
class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val isProcessing = AtomicBoolean(false)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return
        if (isProcessing.get()) return  // Skip if already processing a batch

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val appCtx = ctx.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            // Try to acquire processing lock
            if (!isProcessing.compareAndSet(false, true)) return@launch

            try {
                processBatch(appCtx, messages.mapNotNull { sms ->
                    sms.messageBody?.let { body -> body to sms.timestampMillis }
                })
            } finally {
                isProcessing.set(false)
            }
        }
    }

    private suspend fun processBatch(ctx: Context, messages: List<Pair<String, Long>>) {
        if (messages.isEmpty()) return

        val db = AppDatabase.get(ctx)
        val dao = db.txnDao()
        val accountDao = db.accountDao()
        val merchantDao = db.merchantDao()
        val channelDao = db.paymentChannelDao()

        // Refresh all parser data ONCE per batch (needed if app wasn't launched yet)
        val accounts = accountDao.getAllList()
        val merchants = merchantDao.getAllList()
        val channels = channelDao.getAllList()
        TransactionParser.updateAccounts(accounts)
        TransactionParser.updateMerchants(merchants)
        TransactionParser.updatePaymentChannels(channels)

        // Process sequentially - maintains insertion order
        for ((text, timestamp) in messages) {
            val result = TransactionParser.parse(text, timestamp) ?: continue

            if (result.transaction.amount <= 0) continue

            try {
                dao.insert(result.transaction)

                result.detectedAccountId?.let { accountId ->
                    result.balance?.let { balance ->
                        accountDao.updateBalance(accountId, balance)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error inserting transaction: ${e.message}", e)
                // Re-throw to avoid silent failures in production
                throw e
            }
        }
    }
}
