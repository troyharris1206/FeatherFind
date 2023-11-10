package com.example.featherfind.explore

data class UserSighting(
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
