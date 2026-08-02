package com.boykta.net.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Listens for incoming SMS and extracts OTP codes from messages
 * containing the keyword "Verification Code :".
 * Broadcasts the code locally so the Auth screen can auto-fill the field.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_OTP_RECEIVED = "com.boykta.net.OTP_RECEIVED"
        const val EXTRA_OTP_CODE      = "otp_code"
        private val OTP_PATTERN       = Regex("""Verification Code\s*:\s*(\d{4,8})""", RegexOption.IGNORE_CASE)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val body = msg.messageBody ?: continue
            val match = OTP_PATTERN.find(body) ?: continue
            val otp = match.groupValues[1]

            val broadcast = Intent(ACTION_OTP_RECEIVED).apply {
                putExtra(EXTRA_OTP_CODE, otp)
                setPackage(context.packageName)
            }
            context.sendBroadcast(broadcast)
            return
        }
    }
}
