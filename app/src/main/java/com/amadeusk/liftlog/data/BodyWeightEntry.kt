package com.amadeusk.liftlog.data

/**
 * A single bodyweight log entry.
 *
 * Stored internally in kilograms (kg) just like PRs.
 */
data class BodyWeightEntry(
    val id: Long,      // unique ID, usually System.currentTimeMillis()
    val date: String,  // format: "2025-12-10"
    val weight: Double // stored in KG regardless of UI selection
)
