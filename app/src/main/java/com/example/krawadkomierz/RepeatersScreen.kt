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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class TrainingPhase { READY, COUNTDOWN, WORK, REST, DONE }
enum class Zone { TOO_LOW, GOOD, TOO_HIGH }
fun zoneOf(forceKg: Float, lowKg: Float, highKg: Float): Zone {
    return when {
        forceKg > highKg -> Zone.TOO_HIGH
        forceKg < lowKg -> Zone.TOO_LOW
        else -> Zone.GOOD
    }
}

@Composable
fun RepeatersScreen(settings: AppSettings, modifier: Modifier = Modifier) {
    val targetKg = settings.userMaxKg * settings.targetPercent / 100f
    val lowKg = settings.userMaxKg * (settings.targetPercent - settings.marginPercent) / 100f
    val highKg = settings.userMaxKg * (settings.targetPercent + settings.marginPercent) / 100f

    val phase = remember { mutableStateOf(TrainingPhase.READY) }
    val secondsLeft = remember { mutableStateOf(0) }
    val repetition = remember { mutableStateOf(0) }
    val force = remember { mutableStateOf(0.0f) }
    val readings = remember { mutableStateListOf<Float>() }
    val sampleCount = remember { mutableStateOf(0) }
    val workReadings = remember { mutableStateOf(0) }
    val goodReadings = remember { mutableStateOf(0) }    //jak duzo w zakresie
    val trainingJob = remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    val isRunning = phase.value == TrainingPhase.COUNTDOWN ||
            phase.value == TrainingPhase.WORK ||
            phase.value == TrainingPhase.REST
    val zone = zoneOf(force.value, lowKg, highKg)

    // sila jako sinus zeby cos bylo na wykresie
    LaunchedEffect(phase.value) {
        var time = 0.0
        while (phase.value == TrainingPhase.WORK || phase.value == TrainingPhase.REST) {
            val reading = if (phase.value == TrainingPhase.WORK) {
                targetKg + 4 * sin(time) + Random.nextDouble(-0.5, 0.5)
            } else {
                Random.nextDouble(0.0, 1.0)
            }
            force.value = reading.toFloat()
            readings.add(force.value)
            sampleCount.value++
            if (readings.size > MAX_POINTS) {
                readings.removeAt(0)   // usuwa najstarsze pseudoodczyty
            }
            if (phase.value == TrainingPhase.WORK) {
                workReadings.value++
                if (zoneOf(force.value, lowKg, highKg) == Zone.GOOD) {
                    goodReadings.value++
                }
            }
            time += 0.1
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    val screenColor = when (phase.value) {
        TrainingPhase.WORK -> when (zone) {
            Zone.TOO_HIGH -> Color(0xFFC62828)
            Zone.GOOD -> Color(0xFF2E7D32)
            Zone.TOO_LOW -> Color(0xFFFFEB3B)
        }
        TrainingPhase.REST -> Color(0xFF03A9F4)
        else -> MaterialTheme.colorScheme.background
    }
    val textColor = if (phase.value == TrainingPhase.WORK || phase.value == TrainingPhase.REST) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val message = when (phase.value) {
        TrainingPhase.READY -> "Naciśnij Start"
        TrainingPhase.COUNTDOWN -> "Przygotuj się…"
        TrainingPhase.WORK -> when (zone) {
            Zone.TOO_HIGH -> "Za mocno!"
            Zone.GOOD -> "Dobrze, trzymaj"
            Zone.TOO_LOW -> "Przyłóż się..."
        }
        TrainingPhase.REST -> "Reścik"
        TrainingPhase.DONE -> "Koniec treningu"
    }

    Surface(modifier = modifier.fillMaxSize(), color = screenColor, contentColor = textColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Repeaters", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Cel: %.1f kg (od %.1f do %.1f kg)".format(targetKg, lowKg, highKg))
            Text("Powtórzenie: ${repetition.value} z ${settings.repetitions}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("%.1f kg".format(force.value), fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(message, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (isRunning) {
                Text("Zostało: ${secondsLeft.value} s")
            }
            if (phase.value == TrainingPhase.DONE && workReadings.value > 0) {
                val percentInZone = goodReadings.value * 100 / workReadings.value
                Text("W strefie przez $percentInZone% czasu ciągnięcia")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // wykres na wlasnym Cardzie, nie przyjmuje kolorow
            Surface(shape = RoundedCornerShape(12.dp)) {
                ForceChart(
                    readings = readings,
                    sampleCount = sampleCount.value,
                    zoneLowKg = lowKg,
                    zoneHighKg = highKg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (isRunning) {
                    trainingJob.value?.cancel()
                    phase.value = TrainingPhase.DONE
                } else {
                    trainingJob.value = scope.launch {
                        // 1. Reset
                        readings.clear()
                        sampleCount.value = 0
                        workReadings.value = 0
                        goodReadings.value = 0
                        repetition.value = 0

                        // 2.Odliczanie
                        phase.value = TrainingPhase.COUNTDOWN
                        for (n in settings.countdownSeconds downTo 1) {
                            secondsLeft.value = n
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150)
                            delay(1000)
                        }

                        // 3. Powtorzenia, bez resta po ostatnim
                        for (rep in 1..settings.repetitions) {
                            repetition.value = rep
                            phase.value = TrainingPhase.WORK
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 600)   // pull!
                            for (s in settings.workSeconds downTo 1) {
                                secondsLeft.value = s
                                delay(1000)
                            }
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 400) // let go
                            if (rep < settings.repetitions) {
                                phase.value = TrainingPhase.REST
                                for (s in settings.restSeconds downTo 1) {
                                    secondsLeft.value = s
                                    delay(1000)
                                }
                            }
                        }

                        // 4. Zrobione
                        phase.value = TrainingPhase.DONE
                    }
                }
            }) {
                Text(if (isRunning) "Przerwij" else "Start")
            }
        }
    }
}