package com.amadeusk.liftlog.appui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun ExerciseSelector(
    exercises: List<String>,
    selected: String?,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exercises.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val current = selected ?: exercises.first()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(current)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelectedChange(name)
                        expanded = false
                    }
                )
            }
        }
    }
}
