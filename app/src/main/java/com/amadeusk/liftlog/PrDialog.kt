package com.amadeusk.liftlog

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")
    val allSuggestions = (coreLifts + exerciseSuggestions).distinct()

    var selectedExercise by remember {
        mutableStateOf(if (initialExercise in allSuggestions) initialExercise else "")
    }
    var customExerciseText by remember {
        mutableStateOf(if (initialExercise in allSuggestions) "" else initialExercise)
    }

    var weightText by remember { mutableStateOf(initialWeight) }
    var repsText by remember { mutableStateOf(initialReps) }
    var dateText by remember { mutableStateOf(initialDate) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val weightLabel = if (useKg) "Weight (kg)" else "Weight (lb)"
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // ----- Validation -----
    val isDateValid = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    val weightValue = remember(weightText) { weightText.trim().toDoubleOrNull() }
    val repsValue = remember(repsText) { repsText.trim().toIntOrNull() }

    // require > 0 (feel free to relax if you want 0 allowed)
    val isWeightValid = weightValue != null && weightValue > 0.0
    val isRepsValid = repsValue != null && repsValue > 0

    val canSave = isDateValid && isWeightValid && isRepsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave, // 🔒 Block save unless date+weight+reps valid
                onClick = {
                    val finalExercise =
                        if (customExerciseText.isNotBlank()) customExerciseText.trim()
                        else selectedExercise.trim()

                    onConfirm(
                        finalExercise,
                        weightText.trim(),
                        repsText.trim(),
                        dateText.trim()
                    )
                }
            ) { Text(confirmButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // ---------- Exercise dropdown ----------
                Box(modifier = Modifier.fillMaxWidth()) {

                    OutlinedTextField(
                        value = selectedExercise,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise (from list)") },
                        supportingText = { Text("Tap to pick a common or previous exercise") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Open exercise list"
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
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

                // ---------- Custom exercise ----------
                OutlinedTextField(
                    value = customExerciseText,
                    onValueChange = { customExerciseText = it },
                    label = { Text("Or custom exercise") },
                    supportingText = { Text("Overrides list selection if filled") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ---------- Weight (numeric required) ----------
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(weightLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weightText.isNotBlank() && !isWeightValid,
                    supportingText = {
                        when {
                            weightText.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("OK")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, // allows decimals on most keyboards
                        imeAction = ImeAction.Next
                    )
                )

                // ---------- Reps (integer required) ----------
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = repsText.isNotBlank() && !isRepsValid,
                    supportingText = {
                        when {
                            repsText.isBlank() -> Text("Required")
                            !isRepsValid -> Text("Enter a whole number > 0")
                            else -> Text("OK")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                // ---------- Date (valid ISO required) ----------
                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = dateText,
                    onDateTextChange = { dateText = it }
                )
            }
        }
    )
}

@Composable
private fun DateTextFieldWithCalendar(
    label: String,
    dateText: String,
    onDateTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    val initialDate = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }
            .getOrElse { LocalDate.now(ZoneId.systemDefault()) }
    }

    val isValid = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = onDateTextChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = dateText.isNotBlank() && !isValid,
        supportingText = {
            when {
                dateText.isBlank() -> Text("Required (YYYY-MM-DD)")
                !isValid -> Text("Use format YYYY-MM-DD")
                else -> Text("OK")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val picked = LocalDate.of(year, month + 1, day)
                            onDateTextChange(picked.format(formatter))
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                }
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
            }
        }
    )
}
