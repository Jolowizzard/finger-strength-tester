package com.example.krawadkomierz

import android.content.Context
data class AppSettings(
    val countdownSeconds: Int = 3,
    val maxTestSeconds: Int = 7,
    val rfdTestSeconds: Int = 7,
    val userMaxKg: Int = 50,
    val targetPercent: Int = 80,
    val marginPercent: Int = 5,
    val workSeconds: Int = 7,
    val restSeconds: Int = 3,
    val repetitions: Int = 6,
)

fun loadSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val defaults = AppSettings()
    return AppSettings(
        countdownSeconds = prefs.getInt("countdown_seconds", defaults.countdownSeconds),
        maxTestSeconds = prefs.getInt("max_test_seconds", defaults.maxTestSeconds),
        rfdTestSeconds = prefs.getInt("rfd_test_seconds", defaults.rfdTestSeconds),
        userMaxKg = prefs.getInt("user_max_kg", defaults.userMaxKg),
        targetPercent = prefs.getInt("target_percent", defaults.targetPercent),
        marginPercent = prefs.getInt("margin_percent", defaults.marginPercent),
        workSeconds = prefs.getInt("work_seconds", defaults.workSeconds),
        restSeconds = prefs.getInt("rest_seconds", defaults.restSeconds),
        repetitions = prefs.getInt("repetitions", defaults.repetitions),
    )
}

fun saveSettings(context: Context, settings: AppSettings) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("countdown_seconds", settings.countdownSeconds)
        .putInt("max_test_seconds", settings.maxTestSeconds)
        .putInt("rfd_test_seconds", settings.rfdTestSeconds)
        .putInt("user_max_kg", settings.userMaxKg)
        .putInt("target_percent", settings.targetPercent)
        .putInt("margin_percent", settings.marginPercent)
        .putInt("work_seconds", settings.workSeconds)
        .putInt("rest_seconds", settings.restSeconds)
        .putInt("repetitions", settings.repetitions)
        .apply()
}