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
import com.amadeusk.liftlog.data.BodyWeightEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogApp() {
    val context = LocalContext.current

    // Navigation
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen: LiftLogScreen =
        when (navBackStackEntry?.destination?.route) {
            LiftLogScreen.History.name -> LiftLogScreen.History
            LiftLogScreen.Overview.name,
            null -> LiftLogScreen.Overview
            else -> LiftLogScreen.Overview
        }

    // PRs state
    val prs = remember {
        mutableStateListOf<PR>().apply {
            addAll(loadPrsFromFile(context))
        }
    }

    LaunchedEffect(prs.toList()) {
        savePrsToFile(context, prs)
    }

    // Unit preference
    var unit by remember {
        mutableStateOf(loadUnitPreference(context))
    }

    LaunchedEffect(unit) {
        saveUnitPreference(context, unit)
    }

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }

    // Distinct exercise names for selector
    val exercises = prs.map { it.exercise }.distinct()
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exercises) {
        if (selectedExercise == null && exercises.isNotEmpty()) {
            selectedExercise = exercises.first()
        }
    }

    // Bodyweight list
    val bodyWeights = remember {
        mutableStateListOf<BodyWeightEntry>().apply {
            // if you have persistence:
            // addAll(loadBodyWeightFromFile(context))
        }
    }

    // Save bodyweight when changed (optional if you implement persistence)
    LaunchedEffect(bodyWeights.toList()) {
        // saveBodyWeightToFile(context, bodyWeights)
    }

    var showAddBodyWeightDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentScreen) {
                            LiftLogScreen.Overview -> "LiftLog – Overview"
                            LiftLogScreen.History -> "History"
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

                // Unit toggle visible on all screens
                WeightUnitToggle(
                    current = unit,
                    onUnitChange = { unit = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // -------- NAVHOST AREA --------
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
                            onSelectedExerciseChange = { selectedExercise = it },
                            bodyWeights = bodyWeights,
                            onAddBodyWeightClick = { showAddBodyWeightDialog = true }
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

        if (showAddBodyWeightDialog) {
            AddBodyWeightDialog(
                currentUnit = unit,
                onDismiss = { showAddBodyWeightDialog = false },
                onSave = { entry ->
                    val nextId = (bodyWeights.maxOfOrNull { it.id } ?: 0) + 1
                    bodyWeights.add(entry.copy(id = nextId))
                    showAddBodyWeightDialog = false
                }
            )
        }

    }
}
