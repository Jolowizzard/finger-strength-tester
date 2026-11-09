package com.example.krawadkomierz

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.krawadkomierz.db.HistoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

const val MAX_POINTS = 200           // odczyty na wykresie 10 s
const val SAMPLE_INTERVAL_MS = 50L   // 50 ms

@Composable
fun MeasurementScreen(
    title: String,
    isRfd: Boolean,
    countdownSeconds: Int,
    testSeconds: Int,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel
) {
    val isRunning = remember { mutableStateOf(false) }
    val isMeasuring = remember { mutableStateOf(false) }
    val countdown = remember { mutableStateOf(0) }
    val secondsLeft = remember { mutableStateOf(0) }
    val force = remember { mutableStateOf(0.0f) }
    val maxForce = remember { mutableStateOf(0.0f) }
    val readings = remember { mutableStateListOf<Float>() }
    val sampleCount = remember { mutableStateOf(0) }       // l.odczytow od startu
    val signalSample = remember { mutableStateOf(-1) }     // nr odczytu przy sygnale (-1 = brak)
    val maxSample = remember { mutableStateOf(-1) }        // nr odczytu z max
    val testJob = remember { mutableStateOf<Job?>(null) }  // test w tle (do przerwania)

    val testEnded = remember { mutableStateOf(false) }      // czy test się powiódł, to jest, czy są jakieś dane, które użytkownik może zapisać
    val showSaveDialog = remember {mutableStateOf(false)}   // zmienna wyświetlania okna dialogowego do zapisu
    val scope = rememberCoroutineScope()
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    val dynamometer = rememberDynamometer()

    if(showSaveDialog.value){
        var sessionName by remember { mutableStateOf("Trening Max Force") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveDialog.value = false },
            title = { Text("Zapisz trening") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Wynik max: %.1f kg".format(maxForce.value), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Nazwa treningu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notatka (opcjonalnie)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sessionName.isNotBlank()) {
                            viewModel.saveMaxForceTraining(
                                name = sessionName.trim(),
                                notes = notes.ifBlank { null },
                                maxForceValue = maxForce.value
                            )
                            showSaveDialog.value = false
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog.value = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // zwolnij dźwięk przy wyjściu z ekranu
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // co 50 ms nowy odczyt
    LaunchedEffect(isMeasuring.value) {
        while (isMeasuring.value) {
            force.value = dynamometer.latestKg
            if (force.value > maxForce.value) {
                maxForce.value = force.value
                maxSample.value = sampleCount.value
            }
            readings.add(force.value)
            sampleCount.value++
            if (readings.size > MAX_POINTS) {
                readings.removeAt(0)   // usun najstarszy
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        DynamometerStatus(dynamometer)   // napis: czy silomierz nadaje
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (countdown.value > 0) "${countdown.value}" else "%.1f kg".format(force.value),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Max: %.1f kg".format(maxForce.value))

        // wynik RFD: czas od sygnalu do max
        if (isRfd && !isRunning.value && signalSample.value >= 0 && maxSample.value >= 0) {
            val secondsToMax = (maxSample.value - signalSample.value) * SAMPLE_INTERVAL_MS / 1000f
            Text("Czas do max: %.2f s".format(secondsToMax))
        }

        Text(
            text = when {
                countdown.value > 0 -> "Przygotuj się…"
                secondsLeft.value > 0 -> "Ciągnij! Zostało: ${secondsLeft.value} s"
                maxSample.value >= 0 -> "Koniec – puść"
                else -> "Naciśnij Start"
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        ForceChart(
            readings = readings,
            sampleCount = sampleCount.value,
            signalSample = if (isRfd) signalSample.value else -1,   // kreska tylko w RFD
            maxSample = maxSample.value,                            // kropka max
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(240.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (isRunning.value) {
                // przerwij
                testJob.value?.cancel()
                isRunning.value = false
                isMeasuring.value = false
                countdown.value = 0
                secondsLeft.value = 0
                testEnded.value = false
            } else {
                testJob.value = scope.launch {
                    // 1. zeruj
                    isRunning.value = true
                    readings.clear()
                    sampleCount.value = 0
                    maxForce.value = 0f
                    signalSample.value = -1
                    maxSample.value = -1

                    // 2. RFD nagrywa juz przy odliczaniu
                    if (isRfd) {
                        isMeasuring.value = true
                    }

                    // 3. odliczanie z muzyczka
                    for (n in countdownSeconds downTo 1) {
                        countdown.value = n
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150)
                        delay(1000)
                    }
                    countdown.value = 0

                    // 4. START muzyczka
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 600)
                    signalSample.value = sampleCount.value
                    isMeasuring.value = true

                    // 5. czas testu
                    for (s in testSeconds downTo 1) {
                        secondsLeft.value = s
                        delay(1000)
                    }
                    secondsLeft.value = 0


                    isMeasuring.value = false
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 400)
                    testEnded.value = true
                    isRunning.value = false
                }
            }
        }) {
            Text(if (isRunning.value) "Przerwij" else "Start")
        }
        if(!isRfd) { // Przycisk działa tylko dla widoku MAX_FORCE, narazie tylko do testów
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {showSaveDialog.value = true},
                enabled = testEnded.value && !isRunning.value)
            { Text("Zapisz pomiar")}
        }
    }
}