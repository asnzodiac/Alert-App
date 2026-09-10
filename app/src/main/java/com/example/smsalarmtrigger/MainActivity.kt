package com.example.smsalarmtrigger

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.smsalarmtrigger.data.AlarmSettings
import com.example.smsalarmtrigger.data.PreferencesManager
import com.example.smsalarmtrigger.service.AlarmService
import com.example.smsalarmtrigger.ui.AlarmScreen
import com.example.smsalarmtrigger.ui.theme.SMSAlarmTriggerTheme
import com.example.smsalarmtrigger.util.AudioSyncManager
import com.example.smsalarmtrigger.util.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    private var hasSmsPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var isBatteryOptIgnored by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkPermissionsAndStatus()
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (smsGranted) {
            Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)

        checkPermissionsAndStatus()

        // Fallback sync in case the app is opened before the connectivity
        // callback in SmsAlarmApplication has fired.
        lifecycleScope.launch { AudioSyncManager.syncAll(applicationContext) }

        setContent {
            SMSAlarmTriggerTheme {
                val settings by preferencesManager.settingsFlow
                    .collectAsStateWithLifecycle(initialValue = AlarmSettings())

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen(
                        settings = settings,
                        hasSmsPermission = hasSmsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        isBatteryOptimizationIgnored = isBatteryOptIgnored,
                        onToggleEnabled = { enabled ->
                            lifecycleScope.launch { preferencesManager.setEnabled(enabled) }
                        },
                        onKeywordChanged = { keyword ->
                            lifecycleScope.launch { preferencesManager.setTriggerKeyword(keyword) }
                        },
                        onSenderFilterChanged = { sender ->
                            lifecycleScope.launch { preferencesManager.setSenderFilter(sender) }
                        },
                        onTestAlarm = { audioIndex ->
                            testAlarm(settings.triggerKeyword, audioIndex)
                        },
                        onStopAlarm = {
                            stopAlarm()
                        },
                        onRequestSmsPermission = {
                            requestSmsPermissions()
                        },
                        onRequestNotificationPermission = {
                            requestNotificationPermissions()
                        },
                        onRequestDisableBatteryOptimization = {
                            requestBatteryOptimizationExemption()
                        },
                        onToggleHideIcon = { hidden ->
                            setLauncherIconHidden(hidden)
                            lifecycleScope.launch { preferencesManager.setIconHidden(hidden) }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStatus()
    }

    private fun checkPermissionsAndStatus() {
        hasSmsPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptIgnored = powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestSmsPermissions() {
        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        )
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun testAlarm(keyword: String, audioIndex: Int) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmService.EXTRA_KEYWORD, keyword.ifBlank { "TEST_ALARM" })
            putExtra(AlarmService.EXTRA_SENDER, "")
            putExtra(AlarmService.EXTRA_MESSAGE, "This is a manual test of the SMS Alarm Trigger system.")
            putExtra(AlarmService.EXTRA_AUDIO_INDEX, audioIndex)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopAlarm() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(intent)
    }

    /**
     * Enables/disables the LauncherAlias component, which shows or hides the
     * app icon on the home screen and app drawer. The app itself is untouched
     * and remains listed in Settings > Apps > All apps under its label
     * regardless of this setting.
     */
    private fun setLauncherIconHidden(hidden: Boolean) {
        try {
            val alias = ComponentName(this, "com.example.smsalarmtrigger.LauncherAlias")
            packageManager.setComponentEnabledSetting(
                alias,
                if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't change icon visibility", Toast.LENGTH_SHORT).show()
        }
    }
}
