package com.example.featherfind.explore

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object DataParser {

    fun parseHistogramData(context: Context, fileName: String, apiBirdNames: Set<String>): Map<String, List<Double>> {
        val histogramData = mutableMapOf<String, List<Double>>()
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.readLine()
            reader.readLine()

            reader.forEachLine { line ->
                val tokens = line.split("\t")
                val birdName = tokens[0].trim()

                if (apiBirdNames.contains(birdName)) {
                    val frequencies = tokens.subList(1, tokens.size).map {
                        it.toDoubleOrNull() ?: 0.0
                    }
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

}
