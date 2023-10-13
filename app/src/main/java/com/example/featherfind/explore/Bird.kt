package com.example.featherfind.explore

/**
 * Represents a bird species.
 *
 * @property speciesCode The unique code representing the species of the bird.
 * @property comName The common name of the bird species.
 * @property sciName The scientific name of the bird species.
 * @property histogramData Optional list of histogram data points for the bird species.
 *                         Each point represents a certain characteristic or measurement.
 */
data class Bird(
    val speciesCode: String,
    val comName: String,
    val sciName: String,
    val histogramData: List<Double>? = null
)

/**
 * Represents a geographical hotspot.
 *
 * @property name The name of the hotspot.
 * @property longitude The longitude coordinate of the hotspot.
 * @property latitude The latitude coordinate of the hotspot.
 */
data class Hotspot(
    val name: String,
    val longitude: Double,
    val latitude : Double
)

