package com.example.krawadkomierz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

enum class Screen { HOME, RFD, MAX_FORCE, REPEATERS, HISTORY, SETTINGS }

@Composable
fun App() {
    val currentScreen = remember { mutableStateOf(Screen.HOME) }

    val context = LocalContext.current
    val settings = remember { mutableStateOf(loadSettings(context)) }

    BackHandler(enabled = currentScreen.value != Screen.HOME) {
        currentScreen.value = Screen.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen.value != Screen.HISTORY && currentScreen.value != Screen.SETTINGS,
                    onClick = { currentScreen.value = Screen.HOME },
                    icon = { Text("🧗‍♀️") },
                    label = { Text("Testy") }
                )
                NavigationBarItem(
                    selected = currentScreen.value == Screen.HISTORY,
                    onClick = { currentScreen.value = Screen.HISTORY },
                    icon = { Text("🗓️") },
                    label = { Text("Historia") }
                )
                NavigationBarItem(
                    selected = currentScreen.value == Screen.SETTINGS,
                    onClick = { currentScreen.value = Screen.SETTINGS },
                    icon = { Text("⚙️") },
                    label = { Text("Ustawienia") }
                )
            }
        },
    content = { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        when (currentScreen.value) {
            Screen.HOME -> HomeScreen(
                onTestClick = { screen -> currentScreen.value = screen },
                modifier = screenModifier
            )
            Screen.RFD -> MeasurementScreen(
                title = "RFD",
                isRfd = true,
                countdownSeconds = settings.value.countdownSeconds,
                testSeconds = settings.value.rfdTestSeconds,
                modifier = screenModifier
            )
            Screen.MAX_FORCE -> MeasurementScreen(
                title = "Max Force",
                isRfd = false,
                countdownSeconds = settings.value.countdownSeconds,
                testSeconds = settings.value.maxTestSeconds,
                modifier = screenModifier
            )
            Screen.REPEATERS -> RepeatersScreen(
                settings = settings.value,
                modifier = screenModifier
            )
            Screen.HISTORY -> HistoryScreen(
                modifier = screenModifier
            )
            Screen.SETTINGS -> SettingsScreen(
                settings = settings.value,
                onSettingsChange = { newSettings ->
                    settings.value = newSettings
                    saveSettings(context, newSettings)
                },
                modifier = screenModifier
            )
        }
    })
}