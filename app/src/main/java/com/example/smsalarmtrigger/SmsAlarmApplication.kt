package com.example.smsalarmtrigger

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.example.smsalarmtrigger.util.AudioSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsAlarmApplication : Application() {

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        registerAudioAutoSync()
    }

    /**
     * Registers a callback that fires whenever the device gains a validated
     * internet connection (Wi-Fi turns on, mobile data becomes available,
     * etc.) and kicks off an audio sync check at that moment. Also fires
     * immediately on registration if a usable network already exists, so a
     * normal app launch with connectivity triggers a check too.
     */
    private fun registerAudioAutoSync() {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appScope.launch {
                    AudioSyncManager.syncAll(applicationContext)
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            // Some restricted devices/OEMs may block this; the manual sync
            // still runs from MainActivity on every app launch as a fallback.
        }
    }
}
