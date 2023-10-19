package com.example.featherfind

import com.google.type.DateTime
import java.util.Date

//Data class for birds
data class Birds(
    val birdName: String = "",
    val birdSpecies: String = "",
    val timeOfSighting: String = "",
    val dateOfSighting: String = "",
    val sightingDescription: String = "",
    val photoReference: String = "",
    val userUID: String = ""
)
