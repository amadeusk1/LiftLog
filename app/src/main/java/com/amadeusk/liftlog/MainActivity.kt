package com.amadeusk.liftlog   // <- make sure this matches your namespace

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amadeusk.liftlog.ui.theme.LiftLogTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiftLogTheme {
                LiftLogApp()
            }
        }
    }
}

// ---------- DATA MODEL ----------

data class PR(
    val id: Int,
    val exercise: String,
    val weight: Float,
    val reps: Int,
    val date: String
)

// ---------- FILE PERSISTENCE ----------

private const val PR_FILE_NAME = "prs.txt"

// Save: one PR per line: id|exercise|weight|reps|date
fun savePrsToFile(context: Context, prs: List<PR>) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(PR_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            prs.forEach { pr ->
                val safeExercise = pr.exercise.replace("|", "/") // avoid breaking split
                val line = "${pr.id}|$safeExercise|${pr.weight}|${pr.reps}|${pr.date}"
                writer.write(line)
                writer.write("\n")
            }
        }
    } catch (_: Exception) {
        // ignore for now
    }
}

fun loadPrsFromFile(context: Context): List<PR> {
    val result = mutableListOf<PR>()
    try {
        val input = BufferedReader(
            InputStreamReader(context.openFileInput(PR_FILE_NAME))
        )
        input.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split("|")
                if (parts.size == 5) {
                    val id = parts[0].toIntOrNull() ?: return@forEach
                    val exercise = parts[1]
                    val weight = parts[2].toFloatOrNull() ?: return@forEach
                    val reps = parts[3].toIntOrNull() ?: return@forEach
                    val date = parts[4]
                    result.add(PR(id, exercise, weight, reps, date))
                }
            }
        }
    } catch (_: Exception) {
        // file might not exist yet – that's fine
    }
    return result
}

// ---------- UI ROOT ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogApp() {
    val context = LocalContext.current

    // Load from txt file once, when composable is first created
    val prs = remember {
        mutableStateListOf<PR>().apply {
            addAll(loadPrsFromFile(context))
        }
    }

    // Whenever PR list changes, save to txt file
    LaunchedEffect(prs.toList()) {
        savePrsToFile(context, prs)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    // Distinct exercise names from logged PRs
    val exercises = prs.map { it.exercise }.distinct()
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    // If nothing selected yet but we have exercises, pick the first
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
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                // Exercise selector
                ExerciseSelector(
                    exercises = exercises,
                    selected = selectedExercise,
                    onSelectedChange = { selectedExercise = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Graph for selected exercise
                val currentExercise = selectedExercise
                if (currentExercise != null) {
                    val filtered = prs
                        .filter { it.exercise == currentExercise }
                        .sortedBy { it.date } // yyyy-MM-dd sorts fine as string

                    if (filtered.isNotEmpty()) {
                        Text(
                            text = "Progress for $currentExercise",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        PRLineChart(
                            entries = filtered,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of all PRs
                PRList(
                    prs = prs,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showAddDialog) {
            AddPrDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newPr ->
                    val nextId = (prs.maxOfOrNull { it.id } ?: 0) + 1
                    prs.add(newPr.copy(id = nextId))
                    showAddDialog = false
                }
            )
        }
    }
}

// ---------- EXERCISE SELECTOR ----------

@Composable
fun ExerciseSelector(
    exercises: List<String>,
    selected: String?,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exercises.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val current = selected ?: exercises.first()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(current)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelectedChange(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---------- SIMPLE LINE CHART ----------

@Composable
fun PRLineChart(
    entries: List<PR>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val weights = entries.map { it.weight }
    val minWeight = weights.minOrNull() ?: return
    val maxWeight = weights.maxOrNull() ?: return

    // Avoid flat line when all weights are equal
    val range = (maxWeight - minWeight).takeIf { it != 0f } ?: 1f

    Canvas(modifier = modifier) {
        val padding = 24.dp.toPx()
        val width = size.width - padding * 2
        val height = size.height - padding * 2

        if (entries.size == 1) {
            // Single point – just draw a dot in the middle
            val cx = padding + width / 2
            val cy = padding + height / 2
            drawCircle(
                color = Color(0xFFBB86FC),
                radius = 8f,
                center = Offset(cx, cy)
            )
            return@Canvas
        }

        val stepX = width / (entries.size - 1).coerceAtLeast(1)
        val path = Path()

        entries.forEachIndexed { index, pr ->
            val x = padding + stepX * index
            val normalized = (pr.weight - minWeight) / range
            val y = padding + height - (height * normalized)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // point
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

// ---------- LIST & ROW ----------

@Composable
fun PRList(prs: List<PR>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(prs) { pr ->
            PRRow(pr)
        }
    }
}

@Composable
fun PRRow(pr: PR) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = pr.exercise,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${pr.weight} kg × ${pr.reps} reps")
            Text(text = "Date: ${pr.date}", fontSize = 12.sp)
        }
    }
}

// ---------- ADD PR DIALOG ----------

@Composable
fun AddPrDialog(
    onDismiss: () -> Unit,
    onSave: (PR) -> Unit
) {
    var exercise by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New PR") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Exercise (e.g. Bench Press)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toFloatOrNull()
                    val r = reps.toIntOrNull()

                    if (exercise.isNotBlank() && w != null && r != null) {
                        val date = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date())

                        onSave(
                            PR(
                                id = 0,
                                exercise = exercise.trim(),
                                weight = w,
                                reps = r,
                                date = date
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
