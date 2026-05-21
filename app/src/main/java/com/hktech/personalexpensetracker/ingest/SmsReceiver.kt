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

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(ctx: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent.action}")
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            Log.d("SmsReceiver", "Action mismatch, ignoring")
            return
        }

        val dao = AppDatabase.get(ctx).txnDao()
        val accountDao = AppDatabase.get(ctx).accountDao()

        Log.d("SmsReceiver", "About to extract messages from intent")
        Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms ->
            val body = sms.messageBody ?: return@forEach
            Log.d("SmsReceiver", "SMS body: $body")
            Log.d("SmsReceiver", "Calling TransactionParser.parse...")

            val result = TransactionParser.parse(body, sms.timestampMillis) ?: run {
                Log.d("SmsReceiver", "TransactionParser returned null")
                return@forEach
            }

            Log.d("SmsReceiver", "Parse result: amount=${result.transaction.amount}, merchant=${result.transaction.merchant}, balance=${result.balance}")

            scope.launch {
                try {
                    // Validate transaction before inserting
                    if (result.transaction.amount <= 0) {
                        Log.e("SmsReceiver", "Invalid amount: ${result.transaction.amount}, skipping")
                        return@launch
                    }

                    // Insert transaction
                    dao.insert(result.transaction)
                    Log.d("SmsReceiver", "Transaction inserted successfully: ${result.transaction.id}")

                    // Update account balance if detected
                    result.detectedAccountId?.let { accountId ->
                        result.balance?.let { balance ->
                            accountDao.updateBalance(accountId, balance)
                            Log.d("SmsReceiver", "Updated account $accountId balance to: $balance")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error inserting transaction: ${e.message}", e)
                    // Transaction was silently lost - in production, consider:
                    // - Sending to a retry queue
                    // - Showing notification to user
                    // - Storing in local file for later recovery
                }
            }
        }
    }
}
