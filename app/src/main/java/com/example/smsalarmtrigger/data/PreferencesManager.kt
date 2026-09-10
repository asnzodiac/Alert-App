package com.example.smsalarmtrigger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_alarm_prefs")

data class AlarmSettings(
    val isEnabled: Boolean = true,
    val triggerKeyword: String = "EMERGENCY",
    val senderFilter: String = "",
    val isAlarmActive: Boolean = false,
    val lastTriggeredTime: Long = 0L,
    val lastTriggeredSender: String = "",
    val lastTriggeredBody: String = "",
    val isIconHidden: Boolean = false
)

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_ENABLED = booleanPreferencesKey("is_enabled")
        val KEY_TRIGGER_KEYWORD = stringPreferencesKey("trigger_keyword")
        val KEY_SENDER_FILTER = stringPreferencesKey("sender_filter")
        val KEY_ALARM_ACTIVE = booleanPreferencesKey("is_alarm_active")
        val KEY_LAST_TRIGGERED_TIME = longPreferencesKey("last_triggered_time")
        val KEY_LAST_TRIGGERED_SENDER = stringPreferencesKey("last_triggered_sender")
        val KEY_LAST_TRIGGERED_BODY = stringPreferencesKey("last_triggered_body")
        val KEY_ICON_HIDDEN = booleanPreferencesKey("is_icon_hidden")
    }

    val settingsFlow: Flow<AlarmSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AlarmSettings(
                isEnabled = prefs[KEY_ENABLED] ?: true,
                triggerKeyword = prefs[KEY_TRIGGER_KEYWORD] ?: "EMERGENCY",
                senderFilter = prefs[KEY_SENDER_FILTER] ?: "",
                isAlarmActive = prefs[KEY_ALARM_ACTIVE] ?: false,
                lastTriggeredTime = prefs[KEY_LAST_TRIGGERED_TIME] ?: 0L,
                lastTriggeredSender = prefs[KEY_LAST_TRIGGERED_SENDER] ?: "",
                lastTriggeredBody = prefs[KEY_LAST_TRIGGERED_BODY] ?: "",
                isIconHidden = prefs[KEY_ICON_HIDDEN] ?: false
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setTriggerKeyword(keyword: String) {
        context.dataStore.edit { it[KEY_TRIGGER_KEYWORD] = keyword.trim() }
    }

    suspend fun setSenderFilter(sender: String) {
        context.dataStore.edit { it[KEY_SENDER_FILTER] = sender.trim() }
    }

    suspend fun setAlarmActive(active: Boolean) {
        context.dataStore.edit { it[KEY_ALARM_ACTIVE] = active }
    }

    suspend fun setIconHidden(hidden: Boolean) {
        context.dataStore.edit { it[KEY_ICON_HIDDEN] = hidden }
    }

    suspend fun recordTrigger(sender: String, body: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALARM_ACTIVE] = true
            prefs[KEY_LAST_TRIGGERED_TIME] = timestamp
            prefs[KEY_LAST_TRIGGERED_SENDER] = sender
            prefs[KEY_LAST_TRIGGERED_BODY] = body
        }
    }

    fun getSnapshot(): AlarmSettings = runBlocking {
        settingsFlow.first()
    }
}
