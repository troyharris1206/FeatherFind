package com.example.featherfind.explore

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Singleton object responsible for parsing data files.
 */
object DataParser {

    /**
     * Parses a histogram data file and returns the data in a map.
     *
     * This function reads a file from the assets directory and extracts histogram data
     * for birds whose names match those in the provided set of API bird names.
     *
     * @param context The Android context, used to access the assets directory.
     * @param fileName The name of the file to read from the assets directory.
     * @param apiBirdNames A set of bird names to look for in the file.
     * @return A map from bird names to lists of histogram data.
     */
    fun parseHistogramData(context: Context, fileName: String, apiBirdNames: Set<String>): Map<String, List<Double>> {
        val histogramData = mutableMapOf<String, List<Double>>()

        try {
            // Open the file from the assets directory
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip the first two lines (presumed headers)
            reader.readLine()
            reader.readLine()

            // Parse each subsequent line
            reader.forEachLine { line ->
                val tokens = line.split("\t")
                val birdName = tokens[0].trim()

                // Only include birds that are in the API bird names set
                if (apiBirdNames.contains(birdName)) {
                    // Convert frequency data to doubles, replacing any non-parseable values with 0.0
                    val frequencies = tokens.subList(1, tokens.size).map {
                        it.toDoubleOrNull() ?: 0.0
                    }

                    // Verify the frequency data has the expected length before adding it to the map
                    if (frequencies.size == 49) {
                        histogramData[birdName] = frequencies
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("DataParser", "Error reading histogram data: ${e.message}")
        }
        return histogramData
    }
    /**
     * Parses the raw API response to extract hotspot information and return a list of Hotspot objects.
     * Assumes that the API response is a CSV-like formatted string.
     *
     * @param response Raw API response in String format.
     * @return List of Hotspot objects parsed from the API response.
     */
    fun parseApiResponse(response: String): List<Hotspot> {
        // Split the API response by lines and iterate through each line
        return response.split("\n").mapNotNull { line ->
            // Split each line by comma to extract individual data fields
            val parts = line.split(",")

            // Ensure that each line has at least 8 fields (based on your specific API response structure)
            if (parts.size >= 8) {
                // Extract relevant data fields
                val id = parts[0]
                val name = parts[6]
                val latitude = parts[4].toDoubleOrNull()
                val longitude = parts[5].toDoubleOrNull()

                // Create a Hotspot object if both latitude and longitude can be converted to Double
                if (latitude != null && longitude != null) {
                    Hotspot(latitude, longitude, name)
                } else null // Skip if latitude or longitude can't be converted to Double
            } else null // Skip if the line has fewer than 8 fields
        }
    }
}
