package com.example.smsalarmtrigger.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads updated alert-tone audio files from the GitHub repo whenever the
 * device is online, so the audio files can be swapped out in the repo at any
 * time (a new push to app/src/main/res/raw/audioN.mp3) without needing to
 * reinstall the app.
 *
 * Files are cached in the app's private storage
 * (filesDir/custom_audio/audioN.mp3) and only re-downloaded when the file on
 * GitHub has actually changed, using a conditional HTTP request (ETag) so a
 * sync check costs almost no data if nothing changed.
 *
 * If a repo file hasn't been downloaded yet (first run, offline, etc.),
 * AlarmService falls back to the copy bundled in res/raw at build time.
 */
object AudioSyncManager {

    private const val TAG = "AudioSyncManager"
    private const val PREFS_NAME = "audio_sync_prefs"

    // Raw content base URL for this repo's audio files. Update this if the
    // repo, default branch, or file path ever changes.
    private const val RAW_BASE_URL =
        "https://raw.githubusercontent.com/asnzodiac/Alert_App/main/app/src/main/res/raw/"

    // Add or remove filenames here to control which audio files get synced.
    // Names must match "audio<N>.mp3" to line up with the trigger keyword
    // number (e.g. EMERGENCY2 -> audio2.mp3).
    private val AUDIO_FILES = listOf("audio1.mp3", "audio2.mp3", "audio3.mp3")

    private fun customAudioDir(context: Context): File =
        File(context.filesDir, "custom_audio").apply { if (!exists()) mkdirs() }

    /** The locally cached copy of audio<index>.mp3, if one has been downloaded. */
    fun customAudioFile(context: Context, index: Int): File =
        File(customAudioDir(context), "audio$index.mp3")

    /** Checks GitHub for changed audio files and downloads any that differ from the local cache. */
    suspend fun syncAll(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        for (fileName in AUDIO_FILES) {
            try {
                syncOne(context, prefs, fileName)
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed for $fileName: ${e.message}")
            }
        }
    }

    private fun syncOne(context: Context, prefs: SharedPreferences, fileName: String) {
        val etagKey = "etag_$fileName"
        val storedEtag = prefs.getString(etagKey, null)

        val connection = (URL(RAW_BASE_URL + fileName).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
            if (storedEtag != null) {
                setRequestProperty("If-None-Match", storedEtag)
            }
        }

        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    Log.d(TAG, "$fileName unchanged, skipping download.")
                }
                HttpURLConnection.HTTP_OK -> {
                    val destFile = File(customAudioDir(context), fileName)
                    val tmpFile = File(customAudioDir(context), "$fileName.tmp")
                    connection.inputStream.use { input ->
                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (tmpFile.length() > 0) {
                        if (destFile.exists()) destFile.delete()
                        tmpFile.renameTo(destFile)
                        connection.getHeaderField("ETag")?.let { newEtag ->
                            prefs.edit().putString(etagKey, newEtag).apply()
                        }
                        Log.i(TAG, "$fileName updated (${destFile.length()} bytes).")
                    } else {
                        tmpFile.delete()
                    }
                }
                else -> {
                    Log.w(TAG, "$fileName: unexpected response code $code")
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
