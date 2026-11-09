package com.example.shannosfoodtruck.data

import android.util.Log

/**
 * One place for all app logging, so every message carries the same tag and is easy to
 * filter in Android Studio's Logcat: type  tag:Shannos  in the Logcat search bar.
 *
 * Levels used in this app:
 *   d (debug) – normal flow: screen data loaded, order placed, stage changed
 *   i (info)  – important user events: sign-in, sign-out
 *   w (warn)  – something went wrong but the app recovered
 *   e (error) – an operation failed and the user was told
 *
 * Never log passwords, ID tokens or phone numbers.
 *
 * Reference: Android Developers, "Write and view logs with Logcat",
 * https://developer.android.com/studio/debug/logcat
 */
object AppLog {
    private const val TAG = "Shannos"

    fun d(where: String, message: String) {
        Log.d(TAG, "[$where] $message")
    }

    fun i(where: String, message: String) {
        Log.i(TAG, "[$where] $message")
    }

    fun w(where: String, message: String, error: Throwable? = null) {
        Log.w(TAG, "[$where] $message", error)
    }

    fun e(where: String, message: String, error: Throwable? = null) {
        Log.e(TAG, "[$where] $message", error)
    }
}
