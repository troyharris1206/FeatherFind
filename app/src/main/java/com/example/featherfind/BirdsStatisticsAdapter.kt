package com.example.featherfind

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BirdsStatisticsAdapter(private val birdStatisticsList: List<Pair<String, Int>>) :
    RecyclerView.Adapter<BirdsStatisticsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtMonth: TextView = itemView.findViewById(R.id.txtMonth)
        val txtCount: TextView = itemView.findViewById(R.id.txtCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.statistics_recycler_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (month, count) = birdStatisticsList[position]

        // Convert the month number to month name
        val monthName = getMonthName(month.toInt())
        // Set your TextViews with the month and count
        holder.txtMonth.text = monthName
        holder.txtCount.text = count.toString()
    }

    override fun getItemCount(): Int {
        return birdStatisticsList.size
    }

    private fun getMonthName(month: Int): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return if (month in 1..12) {
            monthNames[month - 1]
        } else {
            ""
        }
    }
}