package com.amadeusk.liftlog.appui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit

@OptIn(ExperimentalTextApi::class)
@Composable
fun PRLineChart(
    entries: List<PR>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    // Convert weights to display unit
    val weights = entries.map { pr ->
        if (unit == WeightUnit.LBS) pr.weight else pr.weight * 0.453592f
    }

    val minWeight = weights.minOrNull() ?: return
    val maxWeight = weights.maxOrNull() ?: return
    val range = (maxWeight - minWeight).takeIf { it != 0f } ?: 1f

    val unitLabel = if (unit == WeightUnit.LBS) "lbs" else "kg"

    Canvas(modifier = modifier) {

        val paddingLeft = 60.dp.toPx()
        val paddingBottom = 40.dp.toPx()
        val paddingTop = 24.dp.toPx()
        val paddingRight = 24.dp.toPx()

        val graphWidth = size.width - paddingLeft - paddingRight
        val graphHeight = size.height - paddingTop - paddingBottom

        // --- Y-axis ---
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // --- X-axis ---
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop + graphHeight),
            end = Offset(paddingLeft + graphWidth, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // Y-axis ticks: min, mid, max
        val yTicks = listOf(minWeight, (minWeight + maxWeight) / 2f, maxWeight)

        yTicks.forEach { value ->
            val normalized = (value - minWeight) / range
            val y = paddingTop + graphHeight - (normalized * graphHeight)

            // Tick mark
            drawLine(
                color = Color.Gray,
                start = Offset(paddingLeft - 10f, y),
                end = Offset(paddingLeft, y),
                strokeWidth = 2f
            )

            // Label
            val label = "%.1f $unitLabel".format(value)
            val textLayout = textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )

            drawText(
                textLayoutResult = textLayout,
                color = Color.Gray,
                topLeft = Offset(
                    paddingLeft - textLayout.size.width - 12f,
                    y - textLayout.size.height / 2f
                )
            )
        }

        // X-axis labels (dates)
        val stepX = graphWidth / (entries.size - 1).coerceAtLeast(1)

        entries.forEachIndexed { index, pr ->
            val x = paddingLeft + stepX * index
            val yBottom = paddingTop + graphHeight

            // Take "MM-DD" if "YYYY-MM-DD", else use full date
            val date = if (pr.date.length >= 10) pr.date.substring(5) else pr.date
            val dateLayout = textMeasurer.measure(
                text = date,
                style = TextStyle(fontSize = 11.sp, color = Color.Gray)
            )

            drawText(
                textLayoutResult = dateLayout,
                color = Color.Gray,
                topLeft = Offset(
                    x - dateLayout.size.width / 2f,
                    yBottom + 8f
                )
            )
        }

        // --- Line + points ---
        val path = Path()

        weights.forEachIndexed { index, w ->
            val x = paddingLeft + stepX * index
            val normalized = (w - minWeight) / range
            val y = paddingTop + graphHeight - (normalized * graphHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = Color(0xFFBB86FC),
                radius = 6f,
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = Color(0xFFBB86FC),
            style = Stroke(width = 4f)
        )
    }
}
