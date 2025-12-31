package com.amadeusk.liftlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.loadBodyWeightsFromFile
import com.amadeusk.liftlog.data.saveBodyWeightsToFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogRoot(viewModel: PRViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentTab by remember { mutableStateOf(LiftLogTab.PRS) }

    var showAddPrDialog by remember { mutableStateOf(false) }
    var prBeingEdited by remember { mutableStateOf<PR?>(null) }

    val exercises = uiState.prs.map { it.exercise }.distinct()

    // 🔹 NEW: always-available core lifts + previous exercises
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")
    val exerciseSuggestions = (coreLifts + exercises).distinct()

    var selectedExercise by remember(exercises) {
        mutableStateOf(exercises.firstOrNull())
    }
    var selectedGraphPr by remember { mutableStateOf<PR?>(null) }
    var selectedRange by remember { mutableStateOf(GraphRange.MONTH) }

    var bodyWeights by remember { mutableStateOf(loadBodyWeightsFromFile(context)) }
    var showAddBwDialog by remember { mutableStateOf(false) }
    var bwBeingEdited by remember { mutableStateOf<BodyWeightEntry?>(null) }
    var selectedBwEntry by remember { mutableStateOf<BodyWeightEntry?>(null) }

    var useKg by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Info page toggle
    var showInfoPage by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LiftLog") },
                navigationIcon = {
                    TextButton(
                        onClick = { showInfoPage = !showInfoPage }
                    ) {
                        Text(if (showInfoPage) "Back" else "Info")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show on PRS / Bodyweight
            if (!showInfoPage && currentTab != LiftLogTab.TOOLS) {
                FloatingActionButton(
                    onClick = {
                        when (currentTab) {
                            LiftLogTab.PRS -> showAddPrDialog = true
                            LiftLogTab.BODYWEIGHT -> showAddBwDialog = true
                            else -> {}
                        }
                    }
                ) {
                    Text("+")
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // If Info is active, show only Info screen
            if (showInfoPage) {
                InfoScreen()
                return@Column
            }

            // ------------------ NORMAL TABS ------------------
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == LiftLogTab.PRS,
                    onClick = { currentTab = LiftLogTab.PRS },
                    text = { Text("PRs") }
                )
                Tab(
                    selected = currentTab == LiftLogTab.BODYWEIGHT,
                    onClick = { currentTab = LiftLogTab.BODYWEIGHT },
                    text = { Text("Bodyweight") }
                )
                Tab(
                    selected = currentTab == LiftLogTab.TOOLS,
                    onClick = { currentTab = LiftLogTab.TOOLS },
                    text = { Text("Tools") }
                )
            }


            // ------------------ MAIN SCREENS ------------------
            when (currentTab) {

                LiftLogTab.PRS -> {
                    if (exercises.isNotEmpty()) {
                        ExerciseSelector(
                            exercises = exercises,
                            selectedExercise = selectedExercise,
                            onExerciseSelected = { exercise ->
                                selectedExercise = exercise
                                selectedGraphPr = null
                            }
                        )

                        GraphRangeSelector(
                            selectedRange = selectedRange,
                            onRangeSelected = { range ->
                                selectedRange = range
                                selectedGraphPr = null
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val prsForSelected = filterPrsByRange(
                            uiState.prs.filter { it.exercise == selectedExercise },
                            selectedRange
                        ).sortedBy { parsePrDateOrMin(it.date) }

                        ExerciseGraph(
                            prs = prsForSelected,
                            selectedPr = selectedGraphPr,
                            onPointSelected = { pr -> selectedGraphPr = pr },
                            useKg = useKg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                        )

                        selectedGraphPr?.let { pr ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(pr.exercise, style = MaterialTheme.typography.titleSmall)
                                    Text("Weight: ${formatWeight(pr.weight, useKg)}")
                                    Text("Reps: ${pr.reps}")
                                    Text("Date: ${pr.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { prBeingEdited = pr }) { Text("Edit") }
                                        TextButton(onClick = {
                                            viewModel.deletePr(pr)
                                            selectedGraphPr = null
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (uiState.prs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("No PRs yet. Tap + to add your first one.") }
                    } else {
                        val history = if (selectedExercise != null) {
                            filterPrsByRange(
                                uiState.prs.filter { it.exercise == selectedExercise },
                                selectedRange
                            ).sortedByDescending { parsePrDateOrMin(it.date) }
                        } else emptyList()

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("No PRs yet for this exercise in this range.") }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it.id }) { pr ->
                                    PRItem(
                                        pr = pr,
                                        useKg = useKg,
                                        onDelete = {
                                            viewModel.deletePr(pr)
                                            if (selectedGraphPr?.id == pr.id) {
                                                selectedGraphPr = null
                                            }
                                        },
                                        onEdit = { prBeingEdited = pr }
                                    )
                                }
                            }
                        }
                    }
                }

                LiftLogTab.BODYWEIGHT -> {
                    if (bodyWeights.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("No bodyweight entries yet. Tap + to add one.") }
                    } else {
                        GraphRangeSelector(
                            selectedRange = selectedRange,
                            onRangeSelected = { range ->
                                selectedRange = range
                                selectedBwEntry = null
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val bwForRange = filterBodyWeightsByRange(bodyWeights, selectedRange)
                            .sortedBy { parsePrDateOrMin(it.date) }

                        BodyWeightGraph(
                            entries = bwForRange,
                            selectedEntry = selectedBwEntry,
                            onPointSelected = { selectedBwEntry = it },
                            useKg = useKg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                        )

                        selectedBwEntry?.let { entry ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Bodyweight", style = MaterialTheme.typography.titleSmall)
                                    Text("Weight: ${formatWeight(entry.weight, useKg)}")
                                    Text("Date: ${entry.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { bwBeingEdited = entry }) { Text("Edit") }
                                        TextButton(onClick = {
                                            bodyWeights = bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)
                                            selectedBwEntry = null
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        val history = bwForRange.sortedByDescending { parsePrDateOrMin(it.date) }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("No bodyweight entries in this range.") }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it.id }) { entry ->
                                    BodyWeightItem(
                                        entry = entry,
                                        useKg = useKg,
                                        onEdit = { bwBeingEdited = entry },
                                        onDelete = {
                                            bodyWeights = bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)
                                            if (selectedBwEntry?.id == entry.id) selectedBwEntry = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                LiftLogTab.TOOLS -> ToolsScreen()
            }
        }
    }

    // --- dialogs unchanged below, except we now pass exerciseSuggestions into PrDialog ---

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Close") }
            },
            title = { Text("Units") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = useKg, onClick = { useKg = true })
                        Text("Kilograms (kg)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !useKg, onClick = { useKg = false })
                        Text("Pounds (lb)")
                    }
                }
            }
        )
    }

    // Add PR dialog
    if (showAddPrDialog) {
        PrDialog(
            title = "Add PR",
            confirmButtonText = "Save",
            initialExercise = "",
            initialWeight = "",
            initialReps = "",
            initialDate = "",
            useKg = useKg,
            exerciseSuggestions = exerciseSuggestions,   // 🔹 NEW
            onDismiss = { showAddPrDialog = false },
            onConfirm = { exercise, weightStr, repsStr, date ->
                val raw = weightStr.toDoubleOrNull() ?: 0.0
                val weightKg = raw.fromDisplayWeight(useKg)

                viewModel.addPr(
                    exercise = exercise,
                    weight = weightKg,
                    reps = repsStr.toIntOrNull() ?: 1,
                    date = date
                )
                showAddPrDialog = false
            }
        )
    }

    // Edit PR dialog
    prBeingEdited?.let { pr ->
        PrDialog(
            title = "Edit PR",
            confirmButtonText = "Update",
            initialExercise = pr.exercise,
            initialWeight = formatWeight(pr.weight, useKg),
            initialReps = pr.reps.toString(),
            initialDate = pr.date,
            useKg = useKg,
            exerciseSuggestions = exerciseSuggestions,   // 🔹 NEW
            onDismiss = { prBeingEdited = null },
            onConfirm = { exercise, weightStr, repsStr, date ->
                val newWeightKg = weightStr.toDoubleOrNull()?.fromDisplayWeight(useKg) ?: pr.weight

                viewModel.updatePr(
                    pr.copy(
                        exercise = exercise,
                        weight = newWeightKg,
                        reps = repsStr.toIntOrNull() ?: pr.reps,
                        date = date
                    )
                )
                prBeingEdited = null
            }
        )
    }

    // Bodyweight dialogs unchanged ...
    if (showAddBwDialog) {
        BodyWeightDialog(
            title = "Add bodyweight",
            confirmButtonText = "Save",
            initialWeight = "",
            initialDate = "",
            useKg = useKg,
            onDismiss = { showAddBwDialog = false },
            onConfirm = { weightStr, date ->
                val raw = weightStr.toDoubleOrNull() ?: 0.0
                val weightKg = raw.fromDisplayWeight(useKg)

                val newEntry = BodyWeightEntry(
                    id = System.currentTimeMillis(),
                    date = date,
                    weight = weightKg
                )
                bodyWeights = bodyWeights + newEntry
                saveBodyWeightsToFile(context, bodyWeights)
                showAddBwDialog = false
            }
        )
    }

    bwBeingEdited?.let { entry ->
        BodyWeightDialog(
            title = "Edit bodyweight",
            confirmButtonText = "Update",
            initialWeight = formatWeight(entry.weight, useKg),
            initialDate = entry.date,
            useKg = useKg,
            onDismiss = { bwBeingEdited = null },
            onConfirm = { weightStr, date ->
                val newWeightKg = weightStr.toDoubleOrNull()?.fromDisplayWeight(useKg) ?: entry.weight

                val updated = entry.copy(weight = newWeightKg, date = date)
                bodyWeights = bodyWeights.map { if (it.id == entry.id) updated else it }
                saveBodyWeightsToFile(context, bodyWeights)
                bwBeingEdited = null
            }
        )
    }
}
