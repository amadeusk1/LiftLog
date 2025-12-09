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
                val safeExercise = pr.exercise.replace("|", "/")
                val line = "${pr.id}|$safeExercise|${pr.weight}|${pr.reps}|${pr.date}"
                writer.write(line)
                writer.write("\n")
            }
        }
    } catch (_: Exception) {}
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
                    val id = parts[0].toIntOrNull() ?: return@forEach
                    val exercise = parts[1]
                    val weight = parts[2].toFloatOrNull() ?: return@forEach
                    val reps = parts[3].toIntOrNull() ?: return@forEach
                    val date = parts[4]
                    result.add(PR(id, exercise, weight, reps, date))
                }
            }
        }
    } catch (_: Exception) {}

    return result
}
