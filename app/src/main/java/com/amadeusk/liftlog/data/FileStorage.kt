package com.amadeusk.liftlog.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val PR_FILE_NAME = "prs.txt"
private const val BW_FILE_NAME = "bw.txt"
private const val TAG = "LiftLogStorage"

// ----------------------
// PR SAVE / LOAD
// ----------------------
fun savePrsToFile(context: Context, prs: List<PR>) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(PR_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            prs.forEach { pr ->
                val safeExercise = pr.exercise.replace("|", "/")
                val line = "${pr.id}|$safeExercise|${pr.weight}|${pr.reps}|${pr.date}"
                writer.write("$line\n")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error saving PRs", e)
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
                    val id = parts[0].toIntOrNull() ?: return@forEach
                    val exercise = parts[1]
                    val weight = parts[2].toFloatOrNull() ?: return@forEach
                    val reps = parts[3].toIntOrNull() ?: return@forEach
                    val date = parts[4]

                    result.add(PR(id, exercise, weight, reps, date))
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading PRs", e)
    }

    return result
}

// ----------------------
// BODYWEIGHT SAVE / LOAD
// ----------------------
fun saveBwToFile(context: Context, bwList: List<BodyWeightEntry>) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(BW_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            bwList.forEach { bw ->
                val line = "${bw.id}|${bw.weightLbs}|${bw.date}"
                writer.write("$line\n")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error saving bodyweight", e)
    }
}

fun loadBwFromFile(context: Context): List<BodyWeightEntry> {
    val result = mutableListOf<BodyWeightEntry>()

    try {
        val input = BufferedReader(
            InputStreamReader(context.openFileInput(BW_FILE_NAME))
        )

        input.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split("|")
                if (parts.size == 3) {
                    val id = parts[0].toIntOrNull() ?: return@forEach
                    val weightLbs = parts[1].toFloatOrNull() ?: return@forEach
                    val date = parts[2]

                    result.add(BodyWeightEntry(id, date, weightLbs))
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading bodyweight", e)
    }

    return result
}
