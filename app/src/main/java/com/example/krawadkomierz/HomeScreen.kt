package com.example.krawadkomierz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.krawadkomierz.ui.theme.KrawądkomierzTheme

@Composable
fun HomeScreen(onTestClick: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Krawądkomierz", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Wybierz test")
        TestCard("RFD", "Jak szybko narasta siła po sygnale") { onTestClick(Screen.RFD) }
        TestCard("Max Force", "Maksymalna siła palców") { onTestClick(Screen.MAX_FORCE) }
        TestCard("Repeaters", "Powtórzenia na wybranym % Twojej siły max") { onTestClick(Screen.REPEATERS) }    }
}

@Composable
fun TestCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(description)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KrawądkomierzTheme {
        HomeScreen(onTestClick = {})
    }
}