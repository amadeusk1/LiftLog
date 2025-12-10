package com.amadeusk.liftlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.ui.theme.LiftLogTheme
import kotlin.math.hypot

class MainActivity : ComponentActivity() {

    private val prViewModel: PRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiftLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PRScreen(prViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRScreen(viewModel: PRViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var prBeingEdited by remember { mutableStateOf<PR?>(null) }

    val exercises = uiState.prs.map { it.exercise }.distinct()
    var selectedExercise by remember(exercises) {
        mutableStateOf(exercises.firstOrNull())
    }

    // Selected point on the graph
    var selectedGraphPr by remember { mutableStateOf<PR?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LiftLog – PR Tracker") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // --- Exercise selector + graph at the top ---
            if (exercises.isNotEmpty()) {
                ExerciseSelector(
                    exercises = exercises,
                    selectedExercise = selectedExercise,
                    onExerciseSelected = { exercise ->
                        selectedExercise = exercise
                        selectedGraphPr = null
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val prsForSelected = uiState.prs
                    .filter { it.exercise == selectedExercise }
                    .sortedBy { it.id }

                ExerciseGraph(
                    prs = prsForSelected,
                    selectedPr = selectedGraphPr,
                    onPointSelected = { pr -> selectedGraphPr = pr },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                )

                // Details of selected point + Edit/Delete buttons
                selectedGraphPr?.let { pr ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = pr.exercise,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(text = "Weight: ${pr.weight} kg")
                            Text(text = "Reps: ${pr.reps}")
                            Text(text = "Date: ${pr.date}")

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { prBeingEdited = pr }
                                ) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.deletePr(pr)
                                        selectedGraphPr = null
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // --- PR history below graph (ONLY selected exercise, NEWEST -> OLDEST) ---
            if (uiState.prs.isEmpty()) {
                // No PRs at all
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No PRs yet. Tap + to add your first one.")
                }
            } else {
                val history = if (selectedExercise != null) {
                    uiState.prs
                        .filter { it.exercise == selectedExercise }
                        .sortedByDescending { it.id }   // NEWEST → OLDEST
                } else {
                    emptyList()
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No PRs yet for this exercise.")
                    }
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

        // Add new PR
        if (showAddDialog) {
            PrDialog(
                title = "Add PR",
                confirmButtonText = "Save",
                initialExercise = "",
                initialWeight = "",
                initialReps = "",
                initialDate = "",
                onDismiss = { showAddDialog = false },
                onConfirm = { exercise, weightStr, repsStr, date ->
                    viewModel.addPr(
                        exercise = exercise,
                        weight = weightStr.toDoubleOrNull() ?: 0.0,
                        reps = repsStr.toIntOrNull() ?: 1,
                        date = date
                    )
                    showAddDialog = false
                }
            )
        }

        // Edit existing PR (from list OR from graph card)
        prBeingEdited?.let { pr ->
            PrDialog(
                title = "Edit PR",
                confirmButtonText = "Update",
                initialExercise = pr.exercise,
                initialWeight = pr.weight.toString(),
                initialReps = pr.reps.toString(),
                initialDate = pr.date,
                onDismiss = { prBeingEdited = null },
                onConfirm = { exercise, weightStr, repsStr, date ->
                    val updated = pr.copy(
                        exercise = exercise,
                        weight = weightStr.toDoubleOrNull() ?: pr.weight,
                        reps = repsStr.toIntOrNull() ?: pr.reps,
                        date = date
                    )
                    viewModel.updatePr(updated)
                    prBeingEdited = null
                }
            )
        }
    }
}

@Composable
fun ExerciseSelector(
    exercises: List<String>,
    selectedExercise: String?,
    onExerciseSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Select exercise", style = MaterialTheme.typography.labelMedium)

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedExercise ?: "Choose exercise")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise) },
                    onClick = {
                        onExerciseSelected(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseGraph(
    prs: List<PR>,
    selectedPr: PR?,
    onPointSelected: (PR) -> Unit,
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

    // Sort by time (id) so the graph goes left → right in order added
    val sorted = remember(prs) { prs.sortedBy { it.id } }

    Column(modifier = modifier) {
        Text(
            text = "PR Progress",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted) {
                    detectTapGestures { tapOffset ->
                        val maxWeight = sorted.maxOf { it.weight }
                        val minWeight = sorted.minOf { it.weight }
                        val weightRange = (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                        val padding = 32f
                        val width = size.width.toFloat() - 2 * padding
                        val height = size.height.toFloat() - 2 * padding
                        val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                        val points = sorted.mapIndexed { index, pr ->
                            val x = padding + stepX * index
                            val normalized = (pr.weight - minWeight) / weightRange
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
                val maxWeight = sorted.maxOf { it.weight }
                val minWeight = sorted.minOf { it.weight }
                val weightRange = (maxWeight - minWeight).takeIf { it != 0.0 } ?: 1.0

                val padding = 32f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding
                val stepX = width / (sorted.size - 1).coerceAtLeast(1)

                val path = Path()

                sorted.forEachIndexed { index, pr ->
                    val x = padding + stepX * index
                    val normalized = (pr.weight - minWeight) / weightRange
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
                    val x = padding + stepX * index
                    val normalized = (pr.weight - minWeight) / weightRange
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
fun PRItem(
    pr: PR,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = pr.exercise, style = MaterialTheme.typography.titleMedium)
                Text(text = "${pr.weight} kg x ${pr.reps} reps")
                Text(text = pr.date, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun PrDialog(
    title: String,
    confirmButtonText: String,
    initialExercise: String,
    initialWeight: String,
    initialReps: String,
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (exercise: String, weight: String, reps: String, date: String) -> Unit
) {
    var exercise by remember { mutableStateOf(initialExercise) }
    var weight by remember { mutableStateOf(initialWeight) }
    var reps by remember { mutableStateOf(initialReps) }
    var date by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(exercise, weight, reps, date) }) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Exercise (e.g. Bench Press)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 2025-12-10)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
