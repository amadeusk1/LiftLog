package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit
import com.amadeusk.liftlog.data.loadPrsFromFile
import com.amadeusk.liftlog.data.savePrsToFile
import com.amadeusk.liftlog.data.loadUnitPreference
import com.amadeusk.liftlog.data.saveUnitPreference

// ---------- simple 2-screen navigation ----------

enum class LiftLogScreen {
    Overview,   // graph page
    History     // list of all entries
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogApp() {
    val context = LocalContext.current

    // Navigation controller
    val navController = rememberNavController()

    // Observe current destination to update top bar label + button text
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen: LiftLogScreen =
        when (navBackStackEntry?.destination?.route) {
            LiftLogScreen.History.name -> LiftLogScreen.History
            LiftLogScreen.Overview.name,
            null -> LiftLogScreen.Overview
            else -> LiftLogScreen.Overview
        }

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

    // Distinct exercise names for selector
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
                title = {
                    Text(
                        when (currentScreen) {
                            LiftLogScreen.Overview -> "LiftLog – Overview"
                            LiftLogScreen.History -> "LiftLog – History"
                        }
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            when (currentScreen) {
                                LiftLogScreen.Overview -> {
                                    navController.navigate(LiftLogScreen.History.name) {
                                        launchSingleTop = true
                                    }
                                }

                                LiftLogScreen.History -> {
                                    navController.navigate(LiftLogScreen.Overview.name) {
                                        launchSingleTop = true
                                        popUpTo(LiftLogScreen.Overview.name) {
                                            inclusive = false
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            when (currentScreen) {
                                LiftLogScreen.Overview -> "History"
                                LiftLogScreen.History -> "Overview"
                            }
                        )
                    }
                }
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

                // Unit toggle shows on both screens
                WeightUnitToggle(
                    current = unit,
                    onUnitChange = { unit = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // ------- NAVHOST AREA -------
                NavHost(
                    navController = navController,
                    startDestination = LiftLogScreen.Overview.name,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(LiftLogScreen.Overview.name) {
                        OverviewScreen(
                            prs = prs,
                            unit = unit,
                            exercises = exercises,
                            selectedExercise = selectedExercise,
                            onSelectedExerciseChange = { selectedExercise = it }
                        )
                    }

                    composable(LiftLogScreen.History.name) {
                        HistoryScreen(
                            prs = prs,
                            unit = unit
                        )
                    }
                }
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

// ---------- Screen Composables used by NavHost ----------

@Composable
private fun OverviewScreen(
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

@Composable
private fun HistoryScreen(
    prs: List<PR>,
    unit: WeightUnit
) {
    PRList(
        prs = prs,
        unit = unit,
        modifier = Modifier.fillMaxSize()
    )
}
