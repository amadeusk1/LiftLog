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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.ui.theme.LiftLogTheme
import kotlin.math.hypot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.platform.LocalContext
import com.amadeusk.liftlog.data.loadBodyWeightsFromFile
import com.amadeusk.liftlog.data.saveBodyWeightsToFile

// ---- Tabs for main content ----
enum class LiftLogTab {
    PRS,
    BODYWEIGHT
}

// ---- Time range for graph/history ----
enum class GraphRange {
    MONTH,
    YEAR,
    ALL
}

private val prDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun filterPrsByRange(prs: List<PR>, range: GraphRange): List<PR> {
    if (range == GraphRange.ALL) return prs

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)   // start of this month
        GraphRange.YEAR -> now.withDayOfYear(1)     // start of this year
        GraphRange.ALL -> LocalDate.MIN
    }

    return prs.filter { pr ->
        val date = try {
            LocalDate.parse(pr.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            // If date can't be parsed, keep it so user data doesn't disappear
            return@filter true
        }
        !date.isBefore(startDate) // date >= startDate
    }
}

// Same style filter for bodyweight entries
private fun filterBodyWeightsByRange(
    entries: List<BodyWeightEntry>,
    range: GraphRange
): List<BodyWeightEntry> {
    if (range == GraphRange.ALL) return entries

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)
        GraphRange.YEAR -> now.withDayOfYear(1)
        GraphRange.ALL -> LocalDate.MIN
    }

    return entries.filter { e ->
        val date = try {
            LocalDate.parse(e.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            return@filter true
        }
        !date.isBefore(startDate)
    }
}

// Parse user-entered date or fallback so ordering still works
private fun parsePrDateOrMin(dateStr: String): LocalDate =
    try {
        LocalDate.parse(dateStr, prDateFormatter)
    } catch (_: Exception) {
        LocalDate.MIN
    }

// ---- Unit helpers ----
private const val KG_TO_LB = 2.2046226
private const val LB_TO_KG = 1.0 / KG_TO_LB

private fun Double.toDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * KG_TO_LB

private fun Double.fromDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * LB_TO_KG

private fun formatWeight(weightKg: Double, useKg: Boolean): String {
    val v = weightKg.toDisplayWeight(useKg)
    return String.format("%.1f", v)
}

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
                    LiftLogRoot(prViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogRoot(viewModel: PRViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Tabs: PRs vs Bodyweight
    var currentTab by remember { mutableStateOf(LiftLogTab.PRS) }

    // PR dialog/edit state
    var showAddPrDialog by remember { mutableStateOf(false) }
    var prBeingEdited by remember { mutableStateOf<PR?>(null) }

    // PR graph state
    val exercises = uiState.prs.map { it.exercise }.distinct()
    var selectedExercise by remember(exercises) {
        mutableStateOf(exercises.firstOrNull())
    }
    var selectedGraphPr by remember { mutableStateOf<PR?>(null) }
    var selectedRange by remember { mutableStateOf(GraphRange.MONTH) }

    // Bodyweight state (PERSISTED via file)
    var bodyWeights by remember {
        mutableStateOf(loadBodyWeightsFromFile(context))
    }
    var showAddBwDialog by remember { mutableStateOf(false) }
    var bwBeingEdited by remember { mutableStateOf<BodyWeightEntry?>(null) }
    var selectedBwEntry by remember { mutableStateOf<BodyWeightEntry?>(null) }

    // Units
    var useKg by remember { mutableStateOf(true) }

    // Settings dialog
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LiftLog") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (currentTab) {
                        LiftLogTab.PRS -> showAddPrDialog = true
                        LiftLogTab.BODYWEIGHT -> showAddBwDialog = true
                    }
                }
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // Tabs row
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
            }

            when (currentTab) {
                LiftLogTab.PRS -> {
                    // ----------------- PR PAGE -----------------
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

                        // Details card for selected point
                        selectedGraphPr?.let { pr ->
                            val unitLabel = if (useKg) "kg" else "lb"
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
                                    Text(
                                        text = "Weight: ${formatWeight(pr.weight, useKg)} $unitLabel"
                                    )
                                    Text(text = "Reps: ${pr.reps}")
                                    Text(text = "Date: ${pr.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { prBeingEdited = pr }) {
                                            Text("Edit")
                                        }
                                        TextButton(onClick = {
                                            viewModel.deletePr(pr)
                                            selectedGraphPr = null
                                        }) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // PR history
                    if (uiState.prs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No PRs yet. Tap + to add your first one.")
                        }
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
                            ) {
                                Text("No PRs yet for this exercise in this range.")
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
                    // ----------------- BODYWEIGHT PAGE -----------------
                    if (bodyWeights.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No bodyweight entries yet. Tap + to add one.")
                        }
                    } else {
                        // Range selector for bodyweight
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
                            onPointSelected = { entry -> selectedBwEntry = entry },
                            useKg = useKg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                        )

                        // Details card for selected bodyweight point
                        selectedBwEntry?.let { entry ->
                            val unitLabel = if (useKg) "kg" else "lb"
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Bodyweight",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Weight: ${formatWeight(entry.weight, useKg)} $unitLabel"
                                    )
                                    Text(text = "Date: ${entry.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { bwBeingEdited = entry }) {
                                            Text("Edit")
                                        }
                                        TextButton(onClick = {
                                            bodyWeights =
                                                bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)
                                            selectedBwEntry = null
                                        }) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // History list for bodyweight (filtered by range, newest -> oldest)
                        val history = bwForRange.sortedByDescending {
                            parsePrDateOrMin(it.date)
                        }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No bodyweight entries in this range.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    history,
                                    key = { it.id }
                                ) { entry ->
                                    BodyWeightItem(
                                        entry = entry,
                                        useKg = useKg,
                                        onEdit = { bwBeingEdited = entry },
                                        onDelete = {
                                            bodyWeights =
                                                bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)
                                            if (selectedBwEntry?.id == entry.id) {
                                                selectedBwEntry = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings dialog (kg / lb)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Units") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = useKg,
                            onClick = { useKg = true }
                        )
                        Text("Kilograms (kg)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !useKg,
                            onClick = { useKg = false }
                        )
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
            onDismiss = { prBeingEdited = null },
            onConfirm = { exercise, weightStr, repsStr, date ->
                val newWeightKg = weightStr.toDoubleOrNull()
                    ?.fromDisplayWeight(useKg)
                    ?: pr.weight

                val updated = pr.copy(
                    exercise = exercise,
                    weight = newWeightKg,
                    reps = repsStr.toIntOrNull() ?: pr.reps,
                    date = date
                )
                viewModel.updatePr(updated)
                prBeingEdited = null
            }
        )
    }

    // Add bodyweight dialog
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

    // Edit bodyweight dialog
    bwBeingEdited?.let { entry ->
        BodyWeightDialog(
            title = "Edit bodyweight",
            confirmButtonText = "Update",
            initialWeight = formatWeight(entry.weight, useKg),
            initialDate = entry.date,
            useKg = useKg,
            onDismiss = { bwBeingEdited = null },
            onConfirm = { weightStr, date ->
                val newWeightKg = weightStr.toDoubleOrNull()
                    ?.fromDisplayWeight(useKg)
                    ?: entry.weight

                val updated = entry.copy(
                    weight = newWeightKg,
                    date = date
                )
                bodyWeights = bodyWeights.map {
                    if (it.id == entry.id) updated else it
                }
                saveBodyWeightsToFile(context, bodyWeights)
                bwBeingEdited = null
            }
        )
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
fun GraphRangeSelector(
    selectedRange: GraphRange,
    onRangeSelected: (GraphRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RangeButton("This month", GraphRange.MONTH, selectedRange, onRangeSelected)
        RangeButton("This year", GraphRange.YEAR, selectedRange, onRangeSelected)
        RangeButton("All time", GraphRange.ALL, selectedRange, onRangeSelected)
    }
}

@Composable
private fun RangeButton(
    label: String,
    range: GraphRange,
    selectedRange: GraphRange,
    onRangeSelected: (GraphRange) -> Unit
) {
    val selected = selectedRange == range
    OutlinedButton(
        onClick = { onRangeSelected(range) },
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

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

@Composable
fun PRItem(
    pr: PR,
    useKg: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val unitLabel = if (useKg) "kg" else "lb"
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
                Text(
                    text = "${formatWeight(pr.weight, useKg)} $unitLabel x ${pr.reps} reps"
                )
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
fun BodyWeightItem(
    entry: BodyWeightEntry,
    useKg: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val unitLabel = if (useKg) "kg" else "lb"
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
                Text("Bodyweight", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatWeight(entry.weight, useKg)} $unitLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.bodySmall
                )
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
    useKg: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (exercise: String, weight: String, reps: String, date: String) -> Unit
) {
    var exercise by remember { mutableStateOf(initialExercise) }
    var weight by remember { mutableStateOf(initialWeight) }
    var reps by remember { mutableStateOf(initialReps) }
    var date by remember { mutableStateOf(initialDate) }

    val unitLabel = if (useKg) "kg" else "lb"

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
                    label = { Text("Weight ($unitLabel)") },
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

@Composable
fun BodyWeightDialog(
    title: String,
    confirmButtonText: String,
    initialWeight: String,
    initialDate: String,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (weight: String, date: String) -> Unit
) {
    var weight by remember { mutableStateOf(initialWeight) }
    var date by remember { mutableStateOf(initialDate) }

    val unitLabel = if (useKg) "kg" else "lb"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(weight, date) }) {
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
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight ($unitLabel)") },
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
