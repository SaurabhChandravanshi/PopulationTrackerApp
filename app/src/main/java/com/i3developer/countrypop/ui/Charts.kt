package com.i3developer.countrypop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

class ChartSeries(val label: String, val color: Color, val points: List<Pair<Int, Long>>)

/** Line chart of population over time. Touch or drag to read the values for a year. */
@Composable
fun PopulationChart(series: List<ChartSeries>, modifier: Modifier = Modifier) {
    val all = series.flatMap { it.points }
    if (all.isEmpty()) return

    val minYear = all.minOf { it.first }
    val maxYear = all.maxOf { it.first }
    val maxValue = niceCeil(all.maxOf { it.second }.toDouble())
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 11.sp)
    var selectedYear by remember(series) { mutableStateOf<Int?>(null) }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(series) {
                    val l = 56.dp.toPx()
                    val r = 8.dp.toPx()
                    detectTapGestures(onPress = { pos ->
                        selectedYear = yearAt(pos.x, size.width.toFloat(), l, r, minYear, maxYear)
                    })
                }
                .pointerInput(series) {
                    val l = 56.dp.toPx()
                    val r = 8.dp.toPx()
                    detectHorizontalDragGestures { change, _ ->
                        selectedYear = yearAt(change.position.x, size.width.toFloat(), l, r, minYear, maxYear)
                    }
                },
        ) {
            val left = 56.dp.toPx()
            val bottom = 22.dp.toPx()
            val top = 8.dp.toPx()
            val plotW = size.width - left - 8.dp.toPx()
            val plotH = size.height - bottom - top
            fun x(year: Int) = left + plotW * (if (maxYear == minYear) 0.5f else (year - minYear).toFloat() / (maxYear - minYear))
            fun y(v: Long) = top + plotH * (1f - (v / maxValue).toFloat())

            for (i in 0..4) {
                val v = maxValue * i / 4
                val yy = top + plotH * (1f - i / 4f)
                drawLine(axisColor, Offset(left, yy), Offset(left + plotW, yy), strokeWidth = 1f)
                val text = measurer.measure(formatCompact(v), labelStyle)
                drawText(text, topLeft = Offset(left - text.size.width - 6.dp.toPx(), yy - text.size.height / 2f))
            }
            val first = measurer.measure(minYear.toString(), labelStyle)
            val last = measurer.measure(maxYear.toString(), labelStyle)
            drawText(first, topLeft = Offset(left - first.size.width / 2f, size.height - first.size.height))
            drawText(last, topLeft = Offset(left + plotW - last.size.width / 2f, size.height - last.size.height))

            for (s in series) {
                val path = Path()
                s.points.forEachIndexed { idx, (yr, v) ->
                    if (idx == 0) path.moveTo(x(yr), y(v)) else path.lineTo(x(yr), y(v))
                }
                drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx()))
            }
            selectedYear?.let { yr ->
                drawLine(labelColor, Offset(x(yr), top), Offset(x(yr), top + plotH), strokeWidth = 1.dp.toPx())
                for (s in series) {
                    val p = s.points.firstOrNull { it.first == yr } ?: continue
                    drawCircle(s.color, 4.dp.toPx(), Offset(x(yr), y(p.second)))
                }
            }
        }
        val yr = selectedYear
        Text(
            text = if (yr == null) {
                "Touch the chart to see a year's figure"
            } else {
                buildString {
                    append(yr)
                    for (s in series) {
                        val p = s.points.firstOrNull { it.first == yr }
                        append("  ·  ")
                        if (series.size > 1) append(s.label).append(": ")
                        append(if (p == null) "no data" else formatFull(p.second))
                    }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun yearAt(px: Float, width: Float, leftPad: Float, rightPad: Float, minYear: Int, maxYear: Int): Int {
    val plot = (width - leftPad - rightPad).coerceAtLeast(1f)
    val frac = ((px - leftPad) / plot).coerceIn(0f, 1f)
    return (minYear + frac * (maxYear - minYear)).roundToInt()
}

/** Rounds up to 1, 2, 2.5, 5 or 10 times a power of ten so axis labels stay tidy. */
private fun niceCeil(v: Double): Double {
    if (v <= 0) return 1.0
    val exp = 10.0.pow(kotlin.math.floor(log10(v)))
    val f = v / exp
    val nice = listOf(1.0, 2.0, 2.5, 5.0, 10.0).firstOrNull { it >= f } ?: 10.0
    return nice * exp
}
