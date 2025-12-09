package com.amadeusk.liftlog.appui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.WeightUnit
import java.util.Date

@Composable
fun AddBodyWeightDialog(
    currentUnit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (BodyWeightEntry) -> Unit
) {
    var weightText by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add bodyweight entry") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Weight (${if (currentUnit == WeightUnit.LBS) "lbs" else "kg"})") }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val value = weightText.text.toFloatOrNull() ?: return@TextButton
                val weightLbs =
                    if (currentUnit == WeightUnit.LBS) value else (value * 2.20462f)

                onSave(
                    BodyWeightEntry(
                        id = 0, // will be replaced by caller
                        date = Date(),
                        weightLbs = weightLbs
                    )
                )
            }) {
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
