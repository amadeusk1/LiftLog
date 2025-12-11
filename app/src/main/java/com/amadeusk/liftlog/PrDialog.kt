package com.amadeusk.liftlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
//import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue          // 🔹 needed for `by remember`
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue        // 🔹 needed for `by remember`
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrDialog(
    title: String,
    confirmButtonText: String,
    initialExercise: String,
    initialWeight: String,
    initialReps: String,
    initialDate: String,
    useKg: Boolean,
    exerciseSuggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (exercise: String, weightStr: String, repsStr: String, date: String) -> Unit
) {
    // Decide how to initialize selection vs custom
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")
    val allSuggestions = (coreLifts + exerciseSuggestions).distinct()

    val initialSelectedFromList = if (initialExercise in allSuggestions) initialExercise else ""
    val initialCustom = if (initialExercise in allSuggestions) "" else initialExercise

    var selectedExercise by remember { mutableStateOf(initialSelectedFromList) }
    var customExerciseText by remember { mutableStateOf(initialCustom) }

    var weightText by remember { mutableStateOf(initialWeight) }
    var repsText by remember { mutableStateOf(initialReps) }
    var dateText by remember { mutableStateOf(initialDate) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val weightLabel = if (useKg) "Weight (kg)" else "Weight (lb)"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val finalExercise = if (customExerciseText.isNotBlank()) {
                        customExerciseText.trim()
                    } else {
                        selectedExercise.trim()
                    }

                    onConfirm(
                        finalExercise,
                        weightText.trim(),
                        repsText.trim(),
                        dateText.trim()
                    )
                }
            ) {
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

                // Dropdown selection (from list)
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedExercise,
                        onValueChange = { /* read-only via menu */ },
                        readOnly = true,
                        label = { Text("Exercise (from list)") },
                        supportingText = {
                            Text("Tap to pick a common or previous exercise")
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        allSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    selectedExercise = suggestion
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Custom exercise text field (takes priority if filled)
                OutlinedTextField(
                    value = customExerciseText,
                    onValueChange = { customExerciseText = it },
                    label = { Text("Or custom exercise name") },
                    supportingText = { Text("If not in the list above, type it here") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(weightLabel) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (e.g. 2025-01-01)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}