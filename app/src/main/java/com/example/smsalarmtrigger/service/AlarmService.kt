package com.example.smsalarmtrigger.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.smsalarmtrigger.data.PreferencesManager
import com.example.smsalarmtrigger.util.AudioSyncManager
import com.example.smsalarmtrigger.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val TAG = "AlarmService"
        const val ACTION_START_ALARM = "com.example.smsalarmtrigger.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.smsalarmtrigger.ACTION_STOP_ALARM"
        const val EXTRA_KEYWORD = "extra_keyword"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_MESSAGE = "extra_message"
        // Int extra. -1 (or absent) means "no number matched, use the default
        // device alarm sound". A positive value N means "play audioN.mp3".
        const val EXTRA_AUDIO_INDEX = "extra_audio_index"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)
        initWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        when (action) {
            ACTION_START_ALARM -> {
                val keyword = intent?.getStringExtra(EXTRA_KEYWORD) ?: "ALARM"
                val sender = intent?.getStringExtra(EXTRA_SENDER) ?: ""
                val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""
                val audioIndex = intent?.getIntExtra(EXTRA_AUDIO_INDEX, -1) ?: -1

                startForegroundWithNotification(keyword)
                boostAlarmVolume()
                startAlarmSound(audioIndex)
                startStrongVibration()

                serviceScope.launch {
                    preferencesManager.setAlarmActive(true)
                    if (sender.isNotBlank() || message.isNotBlank()) {
                        preferencesManager.recordTrigger(sender, message, System.currentTimeMillis())
                    }
                }
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
        }
        return START_STICKY
    }

    // Note: sender is intentionally NOT passed to the notification builder —
    // the alert screen/notification should not surface the phone number.
    private fun startForegroundWithNotification(keyword: String) {
        val notification = NotificationHelper.buildAlarmNotification(this, keyword)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun initWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SMSAlarmTrigger::AlarmWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun boostAlarmVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxAlarmVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error maximizing alarm volume", e)
        }
    }

    /**
     * Resolves which sound to play for a given trigger, in priority order:
     *  1. A custom audio file synced down from the GitHub repo (freshest).
     *  2. The copy of that same file bundled in res/raw at build time.
     *  3. The device's default alarm/notification sound, if no number matched
     *     or no matching file exists in either place.
     */
    private fun startAlarmSound(audioIndex: Int) {
        if (mediaPlayer?.isPlaying == true) return
        try {
            val uri = resolveAlarmSoundUri(audioIndex)
            playUri(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm MediaPlayer for audioIndex=$audioIndex, falling back to default", e)
            try {
                playUri(defaultAlarmUri())
            } catch (e2: Exception) {
                Log.e(TAG, "Error starting default alarm MediaPlayer", e2)
            }
        }
    }

    private fun resolveAlarmSoundUri(audioIndex: Int): Uri {
        if (audioIndex > 0) {
            val downloadedFile = AudioSyncManager.customAudioFile(this, audioIndex)
            if (downloadedFile.exists() && downloadedFile.length() > 0) {
                Log.d(TAG, "Using synced audio file for index $audioIndex")
                return Uri.fromFile(downloadedFile)
            }

            val resId = resources.getIdentifier("audio$audioIndex", "raw", packageName)
            if (resId != 0) {
                Log.d(TAG, "Using bundled raw resource for index $audioIndex")
                return Uri.parse("android.resource://$packageName/$resId")
            }

            Log.w(TAG, "No audio file found for index $audioIndex, falling back to default alarm sound")
        }
        return defaultAlarmUri()
    }

    private fun defaultAlarmUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun playUri(uri: Uri) {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            setVolume(1.0f, 1.0f)
            prepare()
            start()
        }
    }

    private fun startStrongVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating vibrator", e)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        }
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
        serviceScope.launch {
            preferencesManager.setAlarmActive(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
