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
    fun parseApiResponse(response: String): List<Hotspot> {
        // Log the API URL for debugging purposes
        return response.split("\n").mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 8) { // Adjusted index to 8 to ensure all necessary parts are present
                val id = parts[0]
                val name = parts[6]
                val latitude = parts[4].toDoubleOrNull()
                val longitude = parts[5].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    Hotspot(latitude, longitude, name)
                } else null
            } else null
        }
    }

}
