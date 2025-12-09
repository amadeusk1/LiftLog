package com.amadeusk.liftlog.data

data class BodyWeightEntry(
    val id: Int,
    val date: String,      // "yyyy-MM-dd"
    val weightLbs: Float   // always stored in lbs
)
