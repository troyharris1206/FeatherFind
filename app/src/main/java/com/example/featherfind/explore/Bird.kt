package com.example.featherfind.explore

data class Bird(
    val speciesCode: String,
    val comName: String,
    val sciName: String,
    val histogramData: List<Double>? = null
)
data class Hotspot(
    val name: String,
    val longitude: Double,
    val latitude : Double
    // other fields
)

