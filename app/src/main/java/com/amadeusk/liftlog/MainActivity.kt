package com.amadeusk.liftlog   // <- make sure this matches your app's namespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amadeusk.liftlog.ui.theme.LiftLogTheme
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

// ---------- UI ROOT ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogApp() {
    val prs = remember { mutableStateListOf<PR>() }
    var showAddDialog by remember { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (prs.isEmpty()) {
                Text(
                    text = "No PRs yet. Hit the + to log one.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                PRList(prs = prs, modifier = Modifier.fillMaxSize())
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
                                id = 0, // real id set in caller
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
