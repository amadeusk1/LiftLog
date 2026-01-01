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
    val sorted = remember(prs) {
        prs.sortedWith(compareBy<PR> { parsePrDateOrMin(it.date) }.thenBy { it.id })
    }

    ProfessionalLineChart(
        title = "PR Progress",
        items = sorted,
        selected = selectedPr,
        onSelected = onPointSelected,
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) }, // implement below
        formatValue = { v -> formatWeight(v, useKg) },
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
fun BodyWeightGraph(
    entries: List<BodyWeightEntry>,
    selectedEntry: BodyWeightEntry?,
    onPointSelected: (BodyWeightEntry) -> Unit,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    val sorted = remember(entries) {
        entries.sortedWith(compareBy<BodyWeightEntry> { parsePrDateOrMin(it.date) }.thenBy { it.id })
    }

    ProfessionalLineChart(
        title = "Bodyweight Progress",
        items = sorted,
        selected = selectedEntry,
        onSelected = onPointSelected,
        getValue = { it.weight.toDisplayWeight(useKg) },
        getLabel = { shortDateLabel(it.date) },
        formatValue = { v -> formatWeight(v, useKg) },
        modifier = modifier.fillMaxWidth()
    )
}

fun formatWeight(displayWeight: Double, useKg: Boolean): String {
    val unit = if (useKg) "kg" else "lb"
    return "${"%.1f".format(displayWeight)} $unit"
}

// If your stored date is already like "2025-12-17", this will show "12/17".
// If your date format differs, adjust parsing accordingly.
private fun shortDateLabel(date: String): String {
    // dumb-safe fallback: keep last 5 chars if it looks like yyyy-mm-dd
    return if (date.length >= 10 && date[4] == '-' && date[7] == '-') {
        val mm = date.substring(5, 7)
        val dd = date.substring(8, 10)
        "$mm/$dd"
    } else date
}

