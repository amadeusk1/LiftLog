package com.amadeusk.liftlog.data

data class PR(
    val id: Long = System.currentTimeMillis(),
    val exercise: String,
    val weight: Double,
    val reps: Int,
    val date: String // you can later switch this to LocalDate
)
