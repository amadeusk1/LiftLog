package com.amadeusk.liftlog.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val PR_FILE_NAME = "prs.txt"

fun savePrsToFile(context: Context, prs: List<PR>) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(PR_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            prs.forEach { pr ->
                // Make sure the exercise name doesn't break our "|" separator
                val safeExercise = pr.exercise.replace("|", "/")
                val line = "${pr.id}|$safeExercise|${pr.weight}|${pr.reps}|${pr.date}"
                writer.write(line)
                writer.write("\n")
            }
        }
    } catch (_: Exception) {
        // You can log this if you want
    }
}

fun loadPrsFromFile(context: Context): List<PR> {
    val result = mutableListOf<PR>()
    try {
        val input = BufferedReader(
            InputStreamReader(context.openFileInput(PR_FILE_NAME))
        )
        input.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split("|")
                if (parts.size == 5) {
                    val id = parts[0].toLongOrNull() ?: System.currentTimeMillis()
                    val exercise = parts[1]
                    val weight = parts[2].toDoubleOrNull() ?: 0.0
                    val reps = parts[3].toIntOrNull() ?: 0
                    val date = parts[4]

                    result.add(
                        PR(
                            id = id,
                            exercise = exercise,
                            weight = weight,
                            reps = reps,
                            date = date
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
        // If the file doesn't exist yet, just return an empty list
    }
    return result
}
