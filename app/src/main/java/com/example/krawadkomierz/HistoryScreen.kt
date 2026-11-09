package com.example.krawadkomierz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.krawadkomierz.db.HistoryViewModel
import com.example.krawadkomierz.db.MaxForcePoint
import com.example.krawadkomierz.ui.theme.KrawądkomierzTheme
import androidx.compose.ui.graphics.Color
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberUpdatedState
import com.example.krawadkomierz.db.HistoryMode
import kotlinx.coroutines.selects.select

@Composable
fun VicoHistoryChart(
    points: List<MaxForcePoint>,
    onPointSelected: (MaxForcePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    val coroutineScope = rememberCoroutineScope()

    // 1. GWARANCJA AKTUALNOŚCI DANYCH:
    // rememberUpdatedState sprawia, że funkcje zaszyte głęboko w Vico (np. formatter osi)
    // zawsze odczytają najnowszą listę, nawet jeśli Vico nie zaktualizuje swojego stanu.
    val currentPoints by rememberUpdatedState(points)
    val currentOnPointSelected by rememberUpdatedState(onPointSelected)

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = points.mapIndexed { index, _ -> index },
                    y = points.map { it.maxForce }
                )
            }
        }
    }

    val pointComponent = rememberShapeComponent(fill = fill(Color(0xFF1565C0)))

    // 2. LISTENER TWORZONY TYLKO RAZ:
    // Pusta klauzula remember { } powoduje, że obiekt tworzy się raz i nie jest podmieniany
    // w trakcie rysowania wykresu. Dzięki currentPoints w środku, zawsze ma dobre dane.
    val markerVisibilityListener = remember {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val index = targets.firstOrNull()?.x?.toInt() ?: return
                val point = currentPoints.getOrNull(index)
                if (point != null) {
                    coroutineScope.launch {
                        currentOnPointSelected(point)
                    }
                }
            }

            override fun onHidden(marker: CartesianMarker) {}
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(Color(0xFF2E7D32))),
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(pointComponent)
                        )
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = { _, value, _ -> "%.1f kg".format(value) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    val index = value.toInt()
                    // Użycie always-fresh listy
                    val point = currentPoints.getOrNull(index)
                    if (point != null) {
                        dateFormatter.format(Date(point.date))
                    } else " " // Zwróci pusty ciąg, chroniąc przed błędem, jeśli Vico zapyta o usunięty indeks
                }
            ),
            marker = rememberDefaultCartesianMarker(label = rememberTextComponent()),
            markerVisibilityListener = markerVisibilityListener
        ),
        modelProducer = modelProducer,
        modifier = modifier,

        // 3. WYŁĄCZENIE ANIMACJI ZMIAN DANYCH (KLUCZOWE!)
        // Kiedy używasz indeksów (0, 1, 2) jako osi X, usunięcie elementu wszystko przesuwa.
        // Wyłączenie animacji diffing natychmiastowo przerysowuje nowy wykres bez błędogennej interpolacji.
        animationSpec = null
    )
}

@Composable
fun MeasurementDetailCard(
    point: MaxForcePoint,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullDateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = point.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fullDateFormatter.format(Date(point.date)),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "%.1f kg".format(point.maxForce),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!point.notes.isNullOrEmpty()) {
                Text(
                    text = "Notatka: ${point.notes}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Brak notatki",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Usuń pomiar")
            }
        }
    }
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel
) {
    val points by viewModel.maxForcePoints.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()

    // <-- ZMIANA: Stan nie jest na twardo nadpisywany przy każdej zmianie punktów
    var selectedPoint by remember { mutableStateOf<MaxForcePoint?>(null) }
    var pointToDelete by remember { mutableStateOf<MaxForcePoint?>(null) }

    // Zmienna pilnująca, by automatycznie zaznaczyć najnowszy punkt tylko przy pierwszym załadowaniu
    var hasInitialSelectionBeenMade by remember { mutableStateOf(false) }

    LaunchedEffect(points) {
        if (points.isNotEmpty() && !hasInitialSelectionBeenMade) {
            // Automatyczne zaznaczenie przy pierwszym wejściu
            selectedPoint = points.lastOrNull()
            hasInitialSelectionBeenMade = true
        } else if (selectedPoint != null) {
            // Jeśli zaznaczony punkt został usunięty, zdejmij zaznaczenie
            val pointStillExists = points.any { it.sessionId == selectedPoint?.sessionId }
            if (!pointStillExists) {
                selectedPoint = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HistoryModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { newMode -> viewModel.selectMode(newMode)},
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (selectedMode) {
            HistoryMode.MAX_FORCE -> {
                if (points.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Brak zapisanych pomiarów", fontSize = 16.sp)
                    }
                } else {
                    VicoHistoryChart(
                        points = points,
                        onPointSelected = { selectedPoint = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )

                    Text(
                        text = "Kliknij punkt na wykresie, aby zobaczyć szczegóły:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    selectedPoint?.let { currentPoint ->
                        MeasurementDetailCard(
                            point = currentPoint,
                            onDeleteClick = { pointToDelete = currentPoint }
                        )
                    }
                }
            }
            HistoryMode.REPEATERS -> {
                Text("Prace nad tym ekranem wciąż trwają, prosimy o wyrozumiałość")
            }
        }
    }

    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            title = { Text("Usunięcie pomiaru") },
            text = { Text("Czy na pewno chcesz usunąć pomiar '${point.name}' z wynikiem %.1f kg?".format(point.maxForce)) },
            confirmButton = {
                Button(
                    onClick = {
                        val sessionToDeleteId = point.sessionId
                        pointToDelete = null
                        if (selectedPoint?.sessionId == sessionToDeleteId) {
                            selectedPoint = null
                        }
                        viewModel.deleteSession(sessionToDeleteId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pointToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MeasurementDetailCardPreview() {
    KrawądkomierzTheme {
        MeasurementDetailCard(
            point = MaxForcePoint(
                sessionId = 1L,
                date = System.currentTimeMillis(),
                maxForce = 36.8f,
                name = "Max Force Test #1",
                notes = "Dobra forma, uchwyt na krawędzi 12mm"
            ),
            onDeleteClick = {}
        )
    }
}