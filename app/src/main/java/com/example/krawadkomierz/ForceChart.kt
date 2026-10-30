package com.example.krawadkomierz

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

@Composable
fun ForceChart(
    readings: List<Float>,
    sampleCount: Int,
    modifier: Modifier = Modifier,
    signalSample: Int = -1,   // numer odczytu w chwili sygnalu START (-1 - nie pokazuj)
    maxSample: Int = -1,      // numer odczytu z max siłąa
    zoneLowKg: Float = 0f,
    zoneHighKg: Float = 0f
) {
    val secondsPerSample = SAMPLE_INTERVAL_MS / 1000f   // 0.05 s
    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = axisColor.copy(alpha = 0.15f)
    val lineColor = Color(0xFF2E7D32)
    val signalColor = Color(0xFFEA004A)
    val maxColor = Color(0xFF1565C0)
    val zoneColor = Color(0x5566BB6A)
    val labelStyle = TextStyle(fontSize = 11.sp, color = axisColor)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        // max skali Y
        val highestReading = readings.maxOrNull() ?: 0f
        val maxForce = ceil(maxOf(50f, highestReading, zoneHighKg) / 10f) * 10f

        // Plot area = whole canvas minus room for labels
        val left = 36.dp.toPx()
        val top = 24.dp.toPx()
        val right = size.width
        val bottom = size.height - 36.dp.toPx()
        val plotWidth = right - left
        val plotHeight = bottom - top
        val stepX = plotWidth / (MAX_POINTS - 1)

        //czas
        val firstSample = sampleCount - readings.size
        val startTime = firstSample * secondsPerSample
        val windowSeconds = (MAX_POINTS - 1) * secondsPerSample

        // pomocnicze
        fun yOf(kg: Float) = bottom - kg / maxForce * plotHeight
        fun xOf(seconds: Float) = left + (seconds - startTime) / windowSeconds * plotWidth

        // zona celu
        if (zoneHighKg > 0f) {
            val zoneTop = yOf(zoneHighKg)
            val zoneBottom = yOf(zoneLowKg)
            drawRect(zoneColor, topLeft = Offset(left, zoneTop), size = Size(plotWidth, zoneBottom - zoneTop))
        }

        // oś Y
        for (kg in 0..maxForce.toInt() step 10) {
            val y = yOf(kg.toFloat())
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure("$kg", labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y - label.size.height / 2))
        }

        // X
        for (second in ceil(startTime).toInt()..(startTime + windowSeconds).toInt()) {
            if (second % 2 != 0) continue
            val x = xOf(second.toFloat())
            drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure("$second", labelStyle)
            drawText(label, topLeft = Offset(x - label.size.width / 2, bottom + 4.dp.toPx()))
        }

        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 2.dp.toPx())
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2.dp.toPx())

        drawText(textMeasurer.measure("siła [kg]", labelStyle), topLeft = Offset(0f, 0f))
        val timeTitle = textMeasurer.measure("czas [s]", labelStyle)
        drawText(
            timeTitle,
            topLeft = Offset(left + plotWidth / 2 - timeTitle.size.width / 2, size.height - timeTitle.size.height)
        )

        //poczatrk sygnalu
        if (signalSample >= firstSample) {
            val x = left + (signalSample - firstSample) * stepX
            drawLine(
                color = signalColor,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            val label = textMeasurer.measure("sygnał", labelStyle.copy(color = signalColor))
            drawText(label, topLeft = Offset(x + 4.dp.toPx(), top))
        }

        // i sila
        if (readings.size >= 2) {
            val path = Path()
            readings.forEachIndexed { index, value ->
                val x = left + index * stepX
                val y = yOf(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx()))
        }

        // Maximum
        val maxIndex = maxSample - firstSample
        if (maxSample >= 0 && maxIndex in readings.indices) {
            val value = readings[maxIndex]
            val center = Offset(left + maxIndex * stepX, yOf(value))
            drawCircle(maxColor, radius = 5.dp.toPx(), center = center)
            val label = textMeasurer.measure("max %.1f".format(value), labelStyle.copy(color = maxColor))
            drawText(label, topLeft = Offset(center.x - label.size.width / 2, center.y - label.size.height - 6.dp.toPx()))
        }
    }
}