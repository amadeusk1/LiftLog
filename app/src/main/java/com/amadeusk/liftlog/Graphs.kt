package com.amadeusk.liftlog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.BodyWeightEntry
import kotlin.math.hypot

@Composable
fun ExerciseGraph(
    prs: List<PR>,
    selectedPr: PR?,
    onPointSelected: (PR) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    if (prs.size < 2) {
        Column(modifier = modifier) {
            Text(
                text = "Not enough data to draw a graph yet.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    // Sort by user-input date so the graph goes left → right in chronological order
    val sorted = remember(prs) {
        prs.sortedWith(
            compareBy<PR> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    Column(modifier = modifier) {
        Text(
            text = "PR Progress",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted, useKg) {
                    detectTapGestures { tapOffset ->
                        val weights = sorted.map { it.weight.toDisplayWeight(useKg) }
                        val maxWeight = weights.maxOrNull() ?: 0.0
                        val minWeight = weights.minOrNull() ?: 0.0
                        val weightRange =
                            (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                        val padding = 32f
                        val width = size.width.toFloat() - 2 * padding
                        val height = size.height.toFloat() - 2 * padding
                        val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                        val points = sorted.mapIndexed { index, pr ->
                            val w = weights[index]
                            val x = padding + stepX * index
                            val normalized = (w - minWeight) / weightRange
                            val y = padding + (1f - normalized.toFloat()) * height
                            Offset(x, y) to pr
                        }

                        val hit = points.minByOrNull { (center, _) ->
                            hypot(
                                center.x - tapOffset.x,
                                center.y - tapOffset.y
                            )
                        }

                        val hitRadiusPx = 80f // forgiving tap radius
                        if (hit != null) {
                            val (center, pr) = hit
                            val distance = hypot(
                                center.x - tapOffset.x,
                                center.y - tapOffset.y
                            )
                            if (distance <= hitRadiusPx) {
                                onPointSelected(pr)
                            }
                        }
                    }
                }
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val weights = sorted.map { it.weight.toDisplayWeight(useKg) }
                val maxWeight = weights.maxOrNull() ?: 0.0
                val minWeight = weights.minOrNull() ?: 0.0
                val weightRange =
                    (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                val padding = 32f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding
                val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                val path = Path()

                sorted.forEachIndexed { index, pr ->
                    val w = weights[index]
                    val x = padding + stepX * index
                    val normalized = (w - minWeight) / weightRange
                    val y = padding + (1f - normalized.toFloat()) * height

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Line
                drawPath(
                    path = path,
                    color = Color(0xFFBB86FC),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Points
                sorted.forEachIndexed { index, pr ->
                    val w = weights[index]
                    val x = padding + stepX * index
                    val normalized = (w - minWeight) / weightRange
                    val y = padding + (1f - normalized.toFloat()) * height

                    // Outer circle
                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = 10f,
                        center = Offset(x, y)
                    )

                    // Inner circle (highlight if selected)
                    val isSelected = pr == selectedPr
                    drawCircle(
                        color = if (isSelected) Color(0xFFBB86FC) else Color(0xFF000000),
                        radius = if (isSelected) 8f else 6f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun BodyWeightGraph(
    entries: List<BodyWeightEntry>,
    selectedEntry: BodyWeightEntry?,
    onPointSelected: (BodyWeightEntry) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) {
        Column(modifier = modifier) {
            Text(
                text = "Not enough data to draw a graph yet.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val sorted = remember(entries) {
        entries.sortedWith(
            compareBy<BodyWeightEntry> { parsePrDateOrMin(it.date) }
                .thenBy { it.id }
        )
    }

    Column(modifier = modifier) {
        Text(
            text = "Bodyweight Progress",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted, useKg) {
                    detectTapGestures { tapOffset ->
                        val weights = sorted.map { it.weight.toDisplayWeight(useKg) }
                        val maxWeight = weights.maxOrNull() ?: 0.0
                        val minWeight = weights.minOrNull() ?: 0.0
                        val weightRange =
                            (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                        val padding = 32f
                        val width = size.width.toFloat() - 2 * padding
                        val height = size.height.toFloat() - 2 * padding
                        val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                        val points = sorted.mapIndexed { index, entry ->
                            val w = weights[index]
                            val x = padding + stepX * index
                            val normalized = (w - minWeight) / weightRange
                            val y = padding + (1f - normalized.toFloat()) * height
                            Offset(x, y) to entry
                        }

                        val hit = points.minByOrNull { (center, _) ->
                            hypot(
                                center.x - tapOffset.x,
                                center.y - tapOffset.y
                            )
                        }

                        val hitRadiusPx = 80f
                        if (hit != null) {
                            val (center, entry) = hit
                            val distance = hypot(
                                center.x - tapOffset.x,
                                center.y - tapOffset.y
                            )
                            if (distance <= hitRadiusPx) {
                                onPointSelected(entry)
                            }
                        }
                    }
                }
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val weights = sorted.map { it.weight.toDisplayWeight(useKg) }
                val maxWeight = weights.maxOrNull() ?: 0.0
                val minWeight = weights.minOrNull() ?: 0.0
                val weightRange =
                    (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                val padding = 32f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding
                val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                val path = Path()

                sorted.forEachIndexed { index, entry ->
                    val w = weights[index]
                    val x = padding + stepX * index
                    val normalized = (w - minWeight) / weightRange
                    val y = padding + (1f - normalized.toFloat()) * height

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Line
                drawPath(
                    path = path,
                    color = Color(0xFFBB86FC),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Points
                sorted.forEachIndexed { index, entry ->
                    val w = weights[index]
                    val x = padding + stepX * index
                    val normalized = (w - minWeight) / weightRange
                    val y = padding + (1f - normalized.toFloat()) * height

                    drawCircle(
                        color = Color(0xFFFFFFFF),
                        radius = 10f,
                        center = Offset(x, y)
                    )

                    val isSelected = entry == selectedEntry
                    drawCircle(
                        color = if (isSelected) Color(0xFFBB86FC) else Color(0xFF000000),
                        radius = if (isSelected) 8f else 6f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
