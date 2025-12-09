package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit
import com.amadeusk.liftlog.data.loadPrsFromFile
import com.amadeusk.liftlog.data.savePrsToFile
import com.amadeusk.liftlog.data.loadUnitPreference
import com.amadeusk.liftlog.data.saveUnitPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogApp() {
    val context = LocalContext.current

    // Load PRs
    val prs = remember {
        mutableStateListOf<PR>().apply {
            addAll(loadPrsFromFile(context))
        }
    }

    // Save PRs when changed
    LaunchedEffect(prs.toList()) {
        savePrsToFile(context, prs)
    }

    // Load unit preference (LBS default)
    var unit by remember {
        mutableStateOf(loadUnitPreference(context))
    }

    // Save unit preference when changed
    LaunchedEffect(unit) {
        saveUnitPreference(context, unit)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val exercises = prs.map { it.exercise }.distinct()
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exercises) {
        if (selectedExercise == null && exercises.isNotEmpty()) {
            selectedExercise = exercises.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiftLog – PR Log") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->

        if (prs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = "No PRs yet. Hit the + to log one.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Unit toggle (LBS / KG)
                WeightUnitToggle(
                    current = unit,
                    onUnitChange = { unit = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Exercise selector for graph
                ExerciseSelector(
                    exercises = exercises,
                    selected = selectedExercise,
                    onSelectedChange = { selectedExercise = it },
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
                                .height(200.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                PRList(
                    prs = prs,
                    unit = unit,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showAddDialog) {
            AddPrDialog(
                onDismiss = { showAddDialog = false },
                currentUnit = unit,
                onSave = { newPr ->
                    val nextId = (prs.maxOfOrNull { it.id } ?: 0) + 1
                    prs.add(newPr.copy(id = nextId))
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun WeightUnitToggle(
    current: WeightUnit,
    onUnitChange: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Units:")

        FilterChip(
            selected = current == WeightUnit.LBS,
            onClick = { onUnitChange(WeightUnit.LBS) },
            label = { Text("lbs") }
        )
        FilterChip(
            selected = current == WeightUnit.KG,
            onClick = { onUnitChange(WeightUnit.KG) },
            label = { Text("kg") }
        )
    }
}
