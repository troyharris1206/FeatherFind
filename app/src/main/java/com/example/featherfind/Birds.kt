package com.example.featherfind

import com.google.type.DateTime
import java.util.Date

//Data class for birds
data class Birds(
    val birdName: String = "",
    val birdSpecies: String = "",
    val dateOfSighting: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoReference: String = "",
    val sightingDescription: String = "",
    val timeOfSighting: String = "",
    val userUID: String = ""
)
