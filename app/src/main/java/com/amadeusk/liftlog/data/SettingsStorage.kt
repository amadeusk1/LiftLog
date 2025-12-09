package com.amadeusk.liftlog.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val SETTINGS_FILE_NAME = "settings.txt"

fun saveUnitPreference(context: Context, unit: WeightUnit) {
    try {
        val output = OutputStreamWriter(
            context.openFileOutput(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)
        )
        output.use { writer ->
            writer.write(unit.name) // "LBS" or "KG"
        }
    } catch (_: Exception) {
        // ignore
    }
}

fun loadUnitPreference(context: Context): WeightUnit {
    return try {
        val input = BufferedReader(
            InputStreamReader(context.openFileInput(SETTINGS_FILE_NAME))
        )
        val line = input.readLine()?.trim()
        if (line == WeightUnit.KG.name) WeightUnit.KG else WeightUnit.LBS
    } catch (_: Exception) {
        WeightUnit.LBS // default
    }
}
