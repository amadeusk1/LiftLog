package com.amadeusk.liftlog.appui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit

@Composable
fun PRLineChart(
    entries: List<PR>,
    unit: WeightUnit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    // Convert all weights to display unit
    val weights = entries.map { pr ->
        if (unit == WeightUnit.LBS) pr.weight
        else pr.weight * 0.453592f
    }

    val minWeight = weights.minOrNull() ?: return
    val maxWeight = weights.maxOrNull() ?: return
    val range = (maxWeight - minWeight).takeIf { it != 0f } ?: 1f

    Canvas(modifier = modifier) {
        val padding = 24.dp.toPx()
        val width = size.width - padding * 2
        val height = size.height - padding * 2

        if (entries.size == 1) {
            val cx = padding + width / 2
            val cy = padding + height / 2
            drawCircle(
                color = Color(0xFFBB86FC),
                radius = 8f,
                center = Offset(cx, cy)
            )
            return@Canvas
        }

        val path = Path()
        val stepX = width / (entries.size - 1).coerceAtLeast(1)

        weights.forEachIndexed { index, w ->
            val x = padding + stepX * index
            val normalized = (w - minWeight) / range
            val y = padding + height - (height * normalized)

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
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
