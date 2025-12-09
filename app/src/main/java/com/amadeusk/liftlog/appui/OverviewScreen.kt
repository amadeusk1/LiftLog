package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit
import com.amadeusk.liftlog.data.BodyWeightEntry
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OverviewScreen(
    prs: List<PR>,
    unit: WeightUnit,
    exercises: List<String>,
    selectedExercise: String?,
    onSelectedExerciseChange: (String?) -> Unit,
    bodyWeights: List<BodyWeightEntry>,
    onAddBodyWeightClick: () -> Unit
) {
    if (exercises.isEmpty()) {
        // you can still show bodyweight section even if no PRs exist,
        // but I'll keep your “no exercises” behavior as-is
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // -------- PR SECTION (unchanged) --------
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

        Spacer(modifier = Modifier.height(24.dp))

        // -------- BODYWEIGHT SECTION --------
        BodyWeightSection(
            bodyWeights = bodyWeights,
            unit = unit,
            onAddBodyWeightClick = onAddBodyWeightClick
        )
    }
}

@Composable
private fun BodyWeightSection(
    bodyWeights: List<BodyWeightEntry>,
    unit: WeightUnit,
    onAddBodyWeightClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Text(
        text = "Bodyweight (${if (unit == WeightUnit.LBS) "lbs" else "kg"})",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )

    if (bodyWeights.isNotEmpty()) {
        BodyWeightLineChart(
            entries = bodyWeights.sortedBy { it.date },
            unit = unit,
            modifier = Modifier
                .height(220.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        )
    } else {
        Text(
            text = "No bodyweight entries yet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    // Button directly under the graph
    Button(
        onClick = onAddBodyWeightClick,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text("Add bodyweight entry")
    }
}
