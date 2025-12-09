package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit

@Composable
fun OverviewScreen(
    prs: List<PR>,
    unit: WeightUnit,
    exercises: List<String>,
    selectedExercise: String?,
    onSelectedExerciseChange: (String?) -> Unit
) {
    if (exercises.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ExerciseSelector(
            exercises = exercises,
            selected = selectedExercise,
            onSelectedChange = onSelectedExerciseChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val selected = selectedExercise
        if (selected != null) {
            val filtered = prs
                .filter { it.exercise == selected }
                .sortedBy { it.date }

            if (filtered.isNotEmpty()) {
                Text(
                    text = "Progress for $selected (${if (unit == WeightUnit.LBS) "lbs" else "kg"})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                PRLineChart(
                    entries = filtered,
                    unit = unit,
                    modifier = Modifier
                        .height(220.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
