package com.amadeusk.liftlog.appui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.WeightUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun AddBodyWeightDialog(
    currentUnit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (BodyWeightEntry) -> Unit
) {
    var weightText by rememberSaveable { mutableStateOf("") }

    val isValid = weightText.toFloatOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add bodyweight entry") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = {
                    Text(
                        "Weight (${
                            if (currentUnit == WeightUnit.LBS) "lbs" else "kg"
                        })"
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = weightText.toFloatOrNull() ?: return@TextButton

                    val weightLbs =
                        if (currentUnit == WeightUnit.LBS) value
                        else value * 2.20462f

                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateString = formatter.format(Date())

                    onSave(
                        BodyWeightEntry(
                            id = 0,               // caller replaces this
                            date = dateString,    // <-- FIXED!
                            weightLbs = weightLbs
                        )
                    )

                    onDismiss()
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        }
        ,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
