package com.amadeusk.liftlog

import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.BodyWeightEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ---- Tabs for main content ----
enum class LiftLogTab {
    PRS,
    BODYWEIGHT,
    TOOLS
}

// ---- Time range for graph/history ----
enum class GraphRange {
    MONTH,
    YEAR,
    ALL
}

val prDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun filterPrsByRange(prs: List<PR>, range: GraphRange): List<PR> {
    if (range == GraphRange.ALL) return prs

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)   // start of this month
        GraphRange.YEAR -> now.withDayOfYear(1)     // start of this year
        GraphRange.ALL -> LocalDate.MIN
    }

    return prs.filter { pr ->
        val date = try {
            LocalDate.parse(pr.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            // If date can't be parsed, keep it so user data doesn't disappear
            return@filter true
        }
        !date.isBefore(startDate) // date >= startDate
    }
}

// Same style filter for bodyweight entries
fun filterBodyWeightsByRange(
    entries: List<BodyWeightEntry>,
    range: GraphRange
): List<BodyWeightEntry> {
    if (range == GraphRange.ALL) return entries

    val now = LocalDate.now()
    val startDate = when (range) {
        GraphRange.MONTH -> now.withDayOfMonth(1)
        GraphRange.YEAR -> now.withDayOfYear(1)
        GraphRange.ALL -> LocalDate.MIN
    }

    return entries.filter { e ->
        val date = try {
            LocalDate.parse(e.date, prDateFormatter)
        } catch (_: DateTimeParseException) {
            return@filter true
        }
        !date.isBefore(startDate)
    }
}

// Parse user-entered date or fallback so ordering still works
fun parsePrDateOrMin(dateStr: String): LocalDate =
    try {
        LocalDate.parse(dateStr, prDateFormatter)
    } catch (_: Exception) {
        LocalDate.MIN
    }

// ---- Unit helpers ----
const val KG_TO_LB = 2.2046226
const val LB_TO_KG = 1.0 / KG_TO_LB

fun Double.toDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * KG_TO_LB

fun Double.fromDisplayWeight(useKg: Boolean): Double =
    if (useKg) this else this * LB_TO_KG

fun formatWeight(weightKg: Double, useKg: Boolean): String {
    val v = weightKg.toDisplayWeight(useKg)
    return String.format("%.1f", v)
}
