package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.WeightUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddPrDialog(
    onDismiss: () -> Unit,
    currentUnit: WeightUnit,
    onSave: (PR) -> Unit
) {
    val exerciseOptions = listOf("Bench Press", "Squat", "Deadlift", "Other Exercise")

    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(exerciseOptions[0]) }
    var otherExercise by remember { mutableStateOf("") }

    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Exercise dropdown
                Text(text = "Exercise")
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val shownText =
                        if (selectedOption == "Other Exercise" && otherExercise.isNotBlank())
                            otherExercise
                        else
                            selectedOption

                    Text(shownText)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    exerciseOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedOption = option
                                if (option != "Other Exercise") {
                                    otherExercise = ""
                                }
                                expanded = false
                            }
                        )
                    }
                }

                // Custom exercise name if needed
                if (selectedOption == "Other Exercise") {
                    OutlinedTextField(
                        value = otherExercise,
                        onValueChange = { otherExercise = it },
                        label = { Text("Exercise Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Weight & reps fields
                val unitLabel = if (currentUnit == WeightUnit.LBS) "lbs" else "kg"

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight ($unitLabel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val wInput = weight.toFloatOrNull()
                    val r = reps.toIntOrNull()

                    val exerciseName =
                        if (selectedOption == "Other Exercise") otherExercise.trim()
                        else selectedOption

                    if (exerciseName.isNotBlank() && wInput != null && r != null) {
                        // Convert to lbs for storage
                        val weightLbs =
                            if (currentUnit == WeightUnit.LBS) wInput
                            else wInput * 2.20462f

                        val date = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date())

                        onSave(
                            PR(
                                id = 0,
                                exercise = exerciseName,
                                weight = weightLbs,
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
