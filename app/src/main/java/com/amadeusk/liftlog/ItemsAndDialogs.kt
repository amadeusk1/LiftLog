package com.amadeusk.liftlog

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------- Selectors & Range ----------

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

// ---------- List Items ----------

@Composable
fun PRItem(
    pr: PR,
    useKg: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = pr.exercise, style = MaterialTheme.typography.titleMedium)
                Text(text = "${formatWeight(pr.weight, useKg)} x ${pr.reps} reps")
                Text(text = pr.date, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    text = "${formatWeight(entry.weight, useKg)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = entry.date, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

// ---------- Date Field (Manual typing + Calendar Picker) ----------

@Composable
private fun DateTextFieldWithCalendar(
    label: String,
    dateText: String,
    onDateTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE } // yyyy-MM-dd

    val initialDate = remember(dateText) {
        runCatching { LocalDate.parse(dateText.trim(), formatter) }
            .getOrElse { LocalDate.now(ZoneId.systemDefault()) }
    }

    val isValid = remember(dateText) {
        dateText.isNotBlank() && runCatching { LocalDate.parse(dateText.trim(), formatter) }.isSuccess
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = { onDateTextChange(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = dateText.isNotBlank() && !isValid,
        supportingText = {
            when {
                dateText.isBlank() -> Text("Required (YYYY-MM-DD)")
                !isValid -> Text("Use format YYYY-MM-DD")
                else -> Text("")
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
                        { _, year, month, dayOfMonth ->
                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
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

// ---------- Dialogs ----------

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


    // validation
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val isDateValid = remember(date) {
        runCatching { LocalDate.parse(date.trim(), formatter) }.isSuccess
    }
    val weightVal = remember(weight) { weight.trim().toDoubleOrNull() }
    val repsVal = remember(reps) { reps.trim().toIntOrNull() }
    val isWeightValid = weightVal != null && weightVal > 0.0
    val isRepsValid = repsVal != null && repsVal > 0
    val canSave = isDateValid && isWeightValid && isRepsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(exercise, weight, reps, date) }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                    label = { Text("Weight (${if (useKg) "kg" else "lb"})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weight.isNotBlank() && !isWeightValid,
                    supportingText = {
                        when {
                            weight.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = reps.isNotBlank() && !isRepsValid,
                    supportingText = {
                        when {
                            reps.isBlank() -> Text("Required")
                            !isRepsValid -> Text("Enter a whole number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = date,
                    onDateTextChange = { date = it }
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

    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    // ✅ Validation: date must be valid, weight must be numeric > 0
    val isDateValid = remember(date) {
        runCatching { LocalDate.parse(date.trim(), formatter) }.isSuccess
    }
    val weightVal = remember(weight) { weight.trim().toDoubleOrNull() }
    val isWeightValid = weightVal != null && weightVal > 0.0

    val canSave = isDateValid && isWeightValid

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(weight.trim(), date.trim()) }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (${if (useKg) "kg" else "lb"})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weight.isNotBlank() && !isWeightValid,
                    supportingText = {
                        when {
                            weight.isBlank() -> Text("Required")
                            !isWeightValid -> Text("Enter a number > 0")
                            else -> Text("")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                DateTextFieldWithCalendar(
                    label = "Date",
                    dateText = date,
                    onDateTextChange = { date = it }
                )
            }
        }
    )
}
