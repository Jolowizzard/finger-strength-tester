package com.example.krawadkomierz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Ustawienia", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        SectionTitle("Wszystkie testy")
        NumberSetting(
            label = "Odliczanie przed startem",
            value = settings.countdownSeconds, unit = "s", min = 0, max = 10,
            onValueChange = { newValue -> onSettingsChange(settings.copy(countdownSeconds = newValue)) }
        )

        SectionTitle("Max Force")
        NumberSetting(
            label = "Czas testu",
            value = settings.maxTestSeconds, unit = "s", min = 2, max = 15,
            onValueChange = { newValue -> onSettingsChange(settings.copy(maxTestSeconds = newValue)) }
        )

        SectionTitle("RFD")
        NumberSetting(
            label = "Czas po sygnale",
            value = settings.rfdTestSeconds, unit = "s", min = 1, max = 15,
            onValueChange = { newValue -> onSettingsChange(settings.copy(rfdTestSeconds = newValue)) }
        )

        SectionTitle("Repeaters")
        NumberSetting(
            label = "Twoja siła maksymalna",
            value = settings.userMaxKg, unit = "kg", min = 5, max = 150,
            onValueChange = { newValue -> onSettingsChange(settings.copy(userMaxKg = newValue)) }
        )
        NumberSetting(
            label = "Cel (% siły max)",
            value = settings.targetPercent, unit = "%", min = 30, max = 100,
            onValueChange = { newValue -> onSettingsChange(settings.copy(targetPercent = newValue)) }
        )
        NumberSetting(
            label = "Margines (±)",
            value = settings.marginPercent, unit = "%", min = 1, max = 20,
            onValueChange = { newValue -> onSettingsChange(settings.copy(marginPercent = newValue)) }
        )
        NumberSetting(
            label = "Czas ciągnięcia",
            value = settings.workSeconds, unit = "s", min = 3, max = 30,
            onValueChange = { newValue -> onSettingsChange(settings.copy(workSeconds = newValue)) }
        )
        NumberSetting(
            label = "Czas odpoczynku",
            value = settings.restSeconds, unit = "s", min = 1, max = 60,
            onValueChange = { newValue -> onSettingsChange(settings.copy(restSeconds = newValue)) }
        )
        NumberSetting(
            label = "Liczba powtórzeń",
            value = settings.repetitions, unit = "×", min = 1, max = 20,
            onValueChange = { newValue -> onSettingsChange(settings.copy(repetitions = newValue)) }
        )
    }
}
@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
}
@Composable
fun NumberSetting(label: String, value: Int, unit: String, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Button(onClick = { onValueChange(value - 1) }, enabled = value > min) {
            Text("–")
        }
        Text("$value $unit", modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
        Button(onClick = { onValueChange(value + 1) }, enabled = value < max) {
            Text("+")
        }
    }
}