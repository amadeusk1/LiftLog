package com.amadeusk.liftlog.appui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.WeightUnit
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.hypot

@OptIn(ExperimentalTextApi::class)
@Composable
fun BodyWeightLineChart(
    entries: List<BodyWeightEntry>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    // Convert weights to display unit (stored as lbs)
    val weights = entries.map { e ->
        if (unit == WeightUnit.LBS) e.weightLbs else e.weightLbs * 0.453592f
    }

    val minWeight = weights.minOrNull() ?: return
    val maxWeight = weights.maxOrNull() ?: return
    val range = (maxWeight - minWeight).takeIf { it != 0f } ?: 1f

    val unitLabel = if (unit == WeightUnit.LBS) "lbs" else "kg"
    val textMeasurer = rememberTextMeasurer()

    // Incoming format from storage (e.g. "2025-12-09")
    val rawDateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    // Pretty display format (e.g. "Dec 9, 2025")
    val prettyDateFormatter = remember {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }

    var selectedIndex by remember(entries, unit) { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier.pointerInput(entries, unit) {
            detectTapGestures { offset ->
                val paddingLeft = 80.dp.toPx()
                val paddingBottom = 50.dp.toPx()
                val paddingTop = 32.dp.toPx()
                val paddingRight = 40.dp.toPx()

                val graphWidth = size.width - paddingLeft - paddingRight
                val graphHeight = size.height - paddingTop - paddingBottom

                val stepX = if (entries.size > 1) {
                    graphWidth / (entries.size - 1)
                } else {
                    0f
                }

                val points = weights.mapIndexed { index, w ->
                    val x = paddingLeft + stepX * index
                    val normalized = (w - minWeight) / range
                    val y = paddingTop + graphHeight - (normalized * graphHeight)
                    Offset(x, y)
                }

                val hitRadius = 24.dp.toPx()
                val hitIndex = points.indexOfFirst { p ->
                    hypot(p.x - offset.x, p.y - offset.y) <= hitRadius
                }

                selectedIndex = if (hitIndex != -1) hitIndex else null
            }
        }
    ) {
        val paddingLeft = 80.dp.toPx()
        val paddingBottom = 50.dp.toPx()
        val paddingTop = 32.dp.toPx()
        val paddingRight = 40.dp.toPx()

        val graphWidth = size.width - paddingLeft - paddingRight
        val graphHeight = size.height - paddingTop - paddingBottom

        // Axes
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, paddingTop + graphHeight),
            end = Offset(paddingLeft + graphWidth, paddingTop + graphHeight),
            strokeWidth = 2f
        )

        // --- Y label: "Bodyweight (lbs/kg)" rotated 90° ---
        val yLabelText = "Bodyweight ($unitLabel)"
        val yLabelStyle = TextStyle(fontSize = 12.sp, color = Color.Gray)
        val yLabelLayout = textMeasurer.measure(
            text = yLabelText,
            style = yLabelStyle
        )

        // Center of the label near the middle of the Y-axis
        val yLabelCenterX = 24.dp.toPx()                 // distance from left edge
        val yLabelCenterY = paddingTop + graphHeight / 2f

        val yLabelTopLeft = Offset(
            x = yLabelCenterX - yLabelLayout.size.width / 2f,
            y = yLabelCenterY - yLabelLayout.size.height / 2f
        )

        withTransform({
            rotate(
                degrees = -90f,
                pivot = Offset(yLabelCenterX, yLabelCenterY)
            )
        }) {
            drawText(
                textLayoutResult = yLabelLayout,
                topLeft = yLabelTopLeft,
                color = Color.Gray
            )
        }

        // X label: "Date"
        drawText(
            textMeasurer = textMeasurer,
            text = "Date",
            style = TextStyle(fontSize = 12.sp, color = Color.Gray),
            topLeft = Offset(
                paddingLeft + graphWidth / 2f - 20.dp.toPx(),
                paddingTop + graphHeight + 28f
            )
        )

        if (entries.size == 1) {
            val cx = paddingLeft + graphWidth / 2f
            val normalized = (weights[0] - minWeight) / range
            val cy = paddingTop + graphHeight - (normalized * graphHeight)

            drawCircle(
                color = Color(0xFFBB86FC),
                radius = 8f,
                center = Offset(cx, cy)
            )
        } else {
            val path = Path()
            val stepX = graphWidth / (entries.size - 1)

            val points = weights.mapIndexed { index, w ->
                val x = paddingLeft + stepX * index
                val normalized = (w - minWeight) / range
                val y = paddingTop + graphHeight - (normalized * graphHeight)
                Offset(x, y)
            }

            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }

                val isSelected = (index == selectedIndex)

                drawCircle(
                    color = if (isSelected) Color(0xFFFFA726) else Color(0xFFBB86FC),
                    radius = if (isSelected) 9f else 6f,
                    center = point
                )
            }

            drawPath(
                path = path,
                color = Color(0xFFBB86FC),
                style = Stroke(width = 4f)
            )

            // Tooltip
            val idx = selectedIndex
            if (idx != null && idx in entries.indices) {
                val entry = entries[idx]
                val point = points[idx]
                val displayWeight = weights[idx]

                val weightText = "%.1f".format(displayWeight)

                // Convert stored "yyyy-MM-dd" string into "MMM d, yyyy"
                val dateText = try {
                    val parsed = rawDateFormatter.parse(entry.date)
                    if (parsed != null) prettyDateFormatter.format(parsed) else entry.date
                } catch (_: Exception) {
                    entry.date
                }

                val tooltipText = "$weightText $unitLabel\n$dateText"

                val textLayout = textMeasurer.measure(
                    text = tooltipText,
                    style = TextStyle(fontSize = 12.sp, color = Color.White)
                )

                val padding = 6.dp.toPx()
                val boxWidth = textLayout.size.width + padding * 2
                val boxHeight = textLayout.size.height + padding * 2

                var boxX = point.x - boxWidth / 2
                var boxY = point.y - boxHeight - 16f

                if (boxX < 0f) boxX = 0f
                if (boxX + boxWidth > size.width) boxX = size.width - boxWidth
                if (boxY < 0f) boxY = point.y + 16f

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.8f),
                    topLeft = Offset(boxX, boxY),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                drawText(
                    textLayoutResult = textLayout,
                    color = Color.White,
                    topLeft = Offset(boxX + padding, boxY + padding)
                )
            }
        }
    }
}
