package com.amadeusk.liftlog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class ChartPoint<T>(
    val x: Float,
    val y: Float,
    val value: Double,
    val label: String,
    val payload: T
)

@Composable
fun <T> ProfessionalLineChart(
    title: String,
    items: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    getValue: (T) -> Double,
    getLabel: (T) -> String,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (items.size < 2) {
        Column(modifier = modifier) {
            Text("Not enough data to draw a graph yet.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val sorted = remember(items) { items } // pass in already-sorted if needed

    val colors = MaterialTheme.colorScheme
    val lineColor = colors.primary

    // Proper "chart" gridline color: faded gray
    val gridColor = colors.onSurface.copy(alpha = 0.12f)

    val axisTextColor = colors.onSurfaceVariant
    val pointOuter = colors.surface
    val pointInner = colors.onSurface
    val selectedColor = colors.primary

    // Layout constants
    val chartHeight = 220.dp
    val leftGutter = 68.dp     // room for Y labels
    val bottomGutter = 46.dp   // MORE room for X labels (so we can move them down)
    val topGutter = 14.dp
    val rightGutter = 16.dp

    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 18.dp.toPx() }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                var chartPoints by remember { mutableStateOf<List<ChartPoint<T>>>(emptyList()) }

                val selectedIndex = remember(sorted, selected) {
                    if (selected == null) -1 else sorted.indexOfFirst { it == selected }
                }

                // Tooltip
                if (selectedIndex in chartPoints.indices) {
                    val p = chartPoints[selectedIndex]
                    TooltipBubble(
                        text = "${formatValue(p.value)} • ${p.label}",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = with(density) { (p.x / density.density).dp } - 6.dp,
                                y = with(density) { (p.y / density.density).dp } - 36.dp
                            )
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sorted) {
                            detectTapGestures { tap ->
                                val hit = chartPoints.minByOrNull { pt ->
                                    hypot(pt.x - tap.x, pt.y - tap.y)
                                }
                                if (hit != null) {
                                    val d = hypot(hit.x - tap.x, hit.y - tap.y)
                                    if (d <= hitRadiusPx) onSelected(hit.payload)
                                }
                            }
                        }
                ) {
                    val values = sorted.map(getValue)
                    val maxV = values.maxOrNull() ?: 0.0
                    val minV = values.minOrNull() ?: 0.0

                    // Breathing room so line doesn’t touch edges
                    val range = (maxV - minV).takeIf { it != 0.0 } ?: 1.0
                    val paddedMin = minV - range * 0.08
                    val paddedMax = maxV + range * 0.08
                    val paddedRange = (paddedMax - paddedMin).takeIf { it != 0.0 } ?: 1.0

                    val plotLeft = with(density) { leftGutter.toPx() }
                    val plotTop = with(density) { topGutter.toPx() }
                    val plotRight = size.width - with(density) { rightGutter.toPx() }
                    val plotBottom = size.height - with(density) { bottomGutter.toPx() }

                    val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
                    val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

                    val stepX = plotW / (sorted.size - 1).coerceAtLeast(1)

                    // Build points
                    val pts = sorted.mapIndexed { i, item ->
                        val v = getValue(item)
                        val x = plotLeft + stepX * i
                        val t = ((v - paddedMin) / paddedRange).toFloat()
                        val y = plotTop + (1f - t) * plotH
                        ChartPoint(
                            x = x,
                            y = y,
                            value = v,
                            label = getLabel(item),
                            payload = item
                        )
                    }
                    chartPoints = pts

                    // Subtle plot background
                    drawRoundRect(
                        color = colors.surfaceVariant.copy(alpha = 0.25f),
                        topLeft = Offset(plotLeft, plotTop),
                        size = androidx.compose.ui.geometry.Size(plotW, plotH),
                        cornerRadius = CornerRadius(18f, 18f)
                    )

                    // --- GRIDLINES ---
                    // Horizontal grid + Y labels
                    val gridLines = 5
                    for (g in 0..gridLines) {
                        val frac = g / gridLines.toFloat()
                        val y = plotTop + frac * plotH

                        drawLine(
                            color = gridColor,
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = 1.5f
                        )

                        val v = paddedMax - (paddedRange * frac)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 28f
                                color = axisTextColor.toArgb()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                            drawText(
                                formatValue(v),
                                plotLeft - 22f,   // padding to right of Y labels
                                y + 10f,
                                paint
                            )
                        }
                    }

                    // Vertical gridlines (makes it look MUCH more professional)
                    val vLines = 4
                    for (g in 0..vLines) {
                        val frac = g / vLines.toFloat()
                        val x = plotLeft + frac * plotW

                        drawLine(
                            color = gridColor,
                            start = Offset(x, plotTop),
                            end = Offset(x, plotBottom),
                            strokeWidth = 1.5f
                        )
                    }

                    // --- X LABELS ---
                    val targetLabels = 4
                    val stride = (sorted.size / targetLabels).coerceAtLeast(1)
                    val xLabelY = plotBottom + 40f // moved DOWN

                    pts.forEachIndexed { i, p ->
                        if (i % stride == 0 || i == pts.lastIndex) {
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    textSize = 26f
                                    color = axisTextColor.toArgb()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                drawText(
                                    p.label,
                                    p.x,
                                    xLabelY,
                                    paint
                                )
                            }
                        }
                    }

                    // Line path
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    }

                    // Line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Points
                    pts.forEachIndexed { i, p ->
                        val isSelected = (selectedIndex == i)

                        drawCircle(
                            color = pointOuter,
                            radius = if (isSelected) 12f else 10f,
                            center = Offset(p.x, p.y)
                        )
                        drawCircle(
                            color = if (isSelected) selectedColor else pointInner,
                            radius = if (isSelected) 8f else 6f,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
