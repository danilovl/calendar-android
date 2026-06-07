package danilovl.calendar.util

import android.util.Log

object AppLog {
    private const val TAG = "danilovl.calendar"

    fun e(where: String, message: String, t: Throwable? = null) = Log.e(TAG, "[$where] $message", t)
    fun w(where: String, message: String, t: Throwable? = null) = Log.w(TAG, "[$where] $message", t)
    fun i(where: String, message: String) = Log.i(TAG, "[$where] $message")
    fun d(where: String, message: String) = Log.d(TAG, "[$where] $message")
}
