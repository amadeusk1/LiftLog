package com.amadeusk.liftlog.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val BODY_WEIGHT_FILE_NAME = "bodyweight.txt"

/**
 * Save all bodyweight entries to internal storage.
 *
 * Format per line:
 *   id|date|weightKg
 */
fun saveBodyWeightsToFile(context: Context, entries: List<BodyWeightEntry>) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(BODY_WEIGHT_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            entries.forEach { entry ->
                val line = "${entry.id}|${entry.date}|${entry.weight}"
                writer.write(line)
                writer.write("\n")
            }
        }
    } catch (_: Exception) {
        // swallow for now; you can log if you want
    }
}

/**
 * Load all bodyweight entries from internal storage.
 */
fun loadBodyWeightsFromFile(context: Context): List<BodyWeightEntry> {
    val result = mutableListOf<BodyWeightEntry>()
    try {
        val input = BufferedReader(
            InputStreamReader(context.openFileInput(BODY_WEIGHT_FILE_NAME))
        )
        input.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split("|")
                if (parts.size == 3) {
                    val id = parts[0].toLongOrNull() ?: return@forEach
                    val date = parts[1]
                    val weight = parts[2].toDoubleOrNull() ?: return@forEach
                    result.add(
                        BodyWeightEntry(
                            id = id,
                            date = date,
                            weight = weight
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
        // file might not exist yet, that's fine
    }
    return result
}
