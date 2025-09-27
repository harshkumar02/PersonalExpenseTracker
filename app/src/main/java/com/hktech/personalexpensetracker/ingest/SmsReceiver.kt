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
    override fun onReceive(ctx: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val dao = AppDatabase.get(ctx).txnDao()
        val scope = CoroutineScope(Dispatchers.IO)

        Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms ->
            val body = sms.messageBody ?: return@forEach
            Log.d("SmsReceiver", "SMS: $body")
            val parsed = TransactionParser.parse(body, sms.timestampMillis) ?: return@forEach
            scope.launch { dao.insert(parsed) }
        }
    }
}
