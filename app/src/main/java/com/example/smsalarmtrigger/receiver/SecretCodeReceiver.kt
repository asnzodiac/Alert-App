package com.example.smsalarmtrigger.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

/**
 * Listens for the Android "secret code" broadcast (dial *#*#674837#*#* in the
 * Phone app) and re-enables the LauncherAlias component so the app icon
 * reappears on the home screen / app drawer.
 *
 * Support for this broadcast depends on the dialer app in use — it works
 * reliably with the stock/Google Dialer, but some OEM dialers (certain
 * Samsung, Xiaomi builds) don't forward it. If it doesn't work on your
 * device, the icon can also be restored from Settings > Apps > All apps >
 * (the app's disguised name) > Enable, or by temporarily reinstalling.
 */
class SecretCodeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SecretCodeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return

        try {
            val pm = context.packageManager
            val alias = ComponentName(context, "com.example.smsalarmtrigger.LauncherAlias")
            pm.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Toast.makeText(context, "Icon restored", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-enable launcher icon", e)
        }
    }
}
