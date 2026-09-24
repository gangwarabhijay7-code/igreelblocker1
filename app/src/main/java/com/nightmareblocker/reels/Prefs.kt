package com.nightmareblocker.reels

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "nightmare_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SCARE_COUNT = "scare_count"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getScareCount(context: Context): Int =
        prefs(context).getInt(KEY_SCARE_COUNT, 0)

    fun incrementScareCount(context: Context): Int {
        val next = getScareCount(context) + 1
        prefs(context).edit().putInt(KEY_SCARE_COUNT, next).apply()
        return next
    }

    fun resetScareCount(context: Context) {
        prefs(context).edit().putInt(KEY_SCARE_COUNT, 0).apply()
    }
}
