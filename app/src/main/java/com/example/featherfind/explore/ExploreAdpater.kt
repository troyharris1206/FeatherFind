package com.example.featherfind.explore

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.featherfind.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.util.Calendar
import kotlin.math.min

class BirdAdapter : RecyclerView.Adapter<BirdAdapter.BirdViewHolder>() {
    private var birdList: List<Bird> = mutableListOf()

    // ViewHolder class
    class BirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdNameTextView: TextView = itemView.findViewById(R.id.birdNameTextView)
        val birdSciNameTextView: TextView = itemView.findViewById(R.id.birdSciName)
        val barChartContainer: BarChart = itemView.findViewById(R.id.barChart)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirdViewHolder {
        // Inflate the bird_item view
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.bird_item, parent, false)
        return BirdViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: BirdViewHolder, position: Int) {
        val currentBird = birdList[position]
        holder.birdNameTextView.text = currentBird.comName
        holder.birdSciNameTextView.text = currentBird.sciName

        val barChart = holder.barChartContainer
        barChart.clear()  // Clear old data
        barChart.description.isEnabled = false
        val entries = mutableListOf<BarEntry>()

        // If histogramData is available, build the chart
        currentBird.histogramData?.let { data ->
            for (i in 0 until 12) {
                val startIndex = i * 4
                val endIndex = min((i + 1) * 4, data.size)
                if (startIndex < endIndex) {
                    val monthlyAve = data.subList(startIndex, endIndex).average()
                    entries.add(BarEntry(i.toFloat(), monthlyAve.toFloat()))
                }
            }
        }

        if (entries.isNotEmpty()) {
            holder.barChartContainer.visibility = View.VISIBLE  
            val barDataSet = BarDataSet(entries, "Frequency of Species")
            barDataSet.color = Color.GRAY
            barDataSet.setDrawValues(false)
            val barData = BarData(barDataSet)
            barChart.data = barData

            // Create a LimitLine to indicate the current month
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH).toFloat()
            val limitLine = LimitLine(currentMonth)
            limitLine.lineWidth = 1f
            limitLine.lineColor = Color.BLACK
            limitLine.textColor = Color.BLACK
            limitLine.textSize = 12f
            limitLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP

            barChart.setExtraOffsets(0f, 0f, 0f, 0f)
            // Add the LimitLine to the X-axis
            val xAxis = barChart.xAxis
            xAxis.removeAllLimitLines()  // Remove previous lines if any
            xAxis.addLimitLine(limitLine)

            // Set y-axis maximum value to 3
            val yAxisLeft = barChart.axisLeft
            val yAxisRight = barChart.axisRight
            yAxisLeft.axisMaximum = 0.025f
            yAxisRight.axisMaximum = 0.025f
            yAxisLeft.setDrawLabels(false)
            yAxisRight.setDrawLabels(false)

            // Set the custom ValueFormatter for x-axis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = MonthValueFormatter()

            yAxisLeft.setDrawGridLines(false)
            yAxisRight.setDrawGridLines(false)

            // Set the custom ValueFormatter for x-axis
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = MonthValueFormatter()
            xAxis.setLabelCount(12, false) // Show all 12 months
            xAxis.setGranularity(1f) // Set granularity to 1 to show every month

            barChart.invalidate()  // Refresh chart
        } else {
            holder.barChartContainer.visibility = View.GONE  // Hide it
        }
    }

    // Return the size of the dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return birdList.size
    }

    // Update the bird list and notify the adapter to refresh the items
    fun updateData(newBirdList: List<Bird>) {
        birdList = newBirdList.sortedBy {
            it.comName.split(" ").last()
        }
        notifyDataSetChanged()
    }

}
