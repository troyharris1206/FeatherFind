package com.example.featherfind.explore

import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Custom value formatter for displaying month names on the x-axis of a chart.
 *
 * This class extends ValueFormatter to provide custom formatting for month names.
 */
class MonthValueFormatter : ValueFormatter() {
    // Array containing abbreviated month names
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /**
     * Formats the float value to its corresponding month name.
     *
     * @param value The float value representing a month index (0-based).
     * @return The formatted month name.
     */
    override fun getFormattedValue(value: Float): String {
        return monthNames[value.toInt() % monthNames.size]
    }
}
