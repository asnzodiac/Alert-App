package com.example.smsalarmtrigger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.smsalarmtrigger.data.PreferencesManager
import com.example.smsalarmtrigger.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context.applicationContext)
                val settings = preferencesManager.getSnapshot()

                if (!settings.isEnabled) {
                    Log.d(TAG, "SMS Alarm Trigger is disabled by user.")
                    return@launch
                }

                val targetKeyword = settings.triggerKeyword.trim()
                if (targetKeyword.isEmpty()) {
                    Log.d(TAG, "No trigger keyword configured.")
                    return@launch
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    Log.d(TAG, "No SMS messages found in intent.")
                    return@launch
                }

                val sender = messages[0].originatingAddress ?: "Unknown"
                val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

                Log.d(TAG, "Received SMS from: $sender, content length: ${fullBody.length}")

                // 1. Check Sender Restriction (if specified)
                val senderFilter = settings.senderFilter.trim()
                if (senderFilter.isNotEmpty()) {
                    val normalizedSender = normalizePhoneNumber(sender)
                    val normalizedFilter = normalizePhoneNumber(senderFilter)

                    val matchesSender = if (normalizedFilter.isNotEmpty() && normalizedSender.isNotEmpty()) {
                        normalizedSender.endsWith(normalizedFilter) || normalizedFilter.endsWith(normalizedSender)
                    } else {
                        sender.contains(senderFilter, ignoreCase = true)
                    }

                    if (!matchesSender) {
                        Log.d(TAG, "Sender '$sender' does not match filter '$senderFilter'. Ignoring.")
                        return@launch
                    }
                }

                // 2. Check Keyword (case-insensitive), and extract a trailing number if
                //    present (e.g. "EMERGENCY2") to select which alert audio to play.
                val keywordPattern = Regex("(?i)" + Regex.escape(targetKeyword) + "(\\d+)?")
                val match = keywordPattern.find(fullBody)
                if (match == null) {
                    Log.d(TAG, "SMS does not contain trigger keyword '$targetKeyword'.")
                    return@launch
                }
                val audioIndex = match.groupValues.getOrNull(1)?.toIntOrNull() ?: -1

                Log.i(TAG, "Trigger keyword '$targetKeyword' MATCHED (audioIndex=$audioIndex)! Starting AlarmService...")

                // 3. Launch Foreground Service
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START_ALARM
                    putExtra(AlarmService.EXTRA_KEYWORD, targetKeyword)
                    putExtra(AlarmService.EXTRA_SENDER, sender)
                    putExtra(AlarmService.EXTRA_MESSAGE, fullBody)
                    putExtra(AlarmService.EXTRA_AUDIO_INDEX, audioIndex)
                }

                ContextCompat.startForegroundService(context, serviceIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.filter { it.isDigit() }
    }
}
