package com.example.featherfind.explore

import com.github.mikephil.charting.formatter.ValueFormatter

class MonthValueFormatter : ValueFormatter() {
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    override fun getFormattedValue(value: Float): String {
        return monthNames[value.toInt() % monthNames.size]
    }
}
