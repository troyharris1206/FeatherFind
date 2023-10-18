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

/**
 * The BirdAdapter class is responsible for binding the bird data to the RecyclerView.
 */
class BirdAdapter : RecyclerView.Adapter<BirdAdapter.BirdViewHolder>() {

    // Holds the list of birds to be displayed in the RecyclerView.
    private var birdList: List<Bird> = mutableListOf()

    /**
     * ViewHolder class to cache view references for each bird item in the RecyclerView.
     */
    class BirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdNameTextView: TextView = itemView.findViewById(R.id.birdNameTextView)
        val birdSciNameTextView: TextView = itemView.findViewById(R.id.birdSciName)
        val barChartContainer: BarChart = itemView.findViewById(R.id.barChart)
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of a given type to represent an item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirdViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.bird_item, parent, false)
        return BirdViewHolder(itemView)
    }

    /**
     * Called by RecyclerView to display the data at a specified position.
     */
    override fun onBindViewHolder(holder: BirdViewHolder, position: Int) {
        // Populate the view with bird data.
        val currentBird = birdList[position]
        holder.birdNameTextView.text = currentBird.comName
        holder.birdSciNameTextView.text = currentBird.sciName

        // Set up the bar chart.
        setupBarChart(holder.barChartContainer, currentBird.histogramData?.map { it.toFloat() })

    }

    /**
     * Returns the number of items in the bird list.
     */
    override fun getItemCount(): Int {
        return birdList.size
    }

    /**
     * Updates the bird list with new data and sorts it based on the last word in the common name.
     *
     * @param newBirdList The updated list of birds.
     */
    fun updateData(newBirdList: List<Bird>) {
        birdList = newBirdList.sortedBy { it.comName.split(" ").last() }
        notifyDataSetChanged()
    }

    /**
     * Helper function to set up the bar chart.
     *
     * @param barChart The BarChart object to be set up.
     * @param histogramData The histogram data for the bird.
     */
    private fun setupBarChart(barChart: BarChart, histogramData: List<Float>?) {
        // Clear old data.
        barChart.clear()
        barChart.description.isEnabled = false

        // Prepare new data entries.
        val entries = mutableListOf<BarEntry>()  // Declare entries here

        // Prepare new data entries.
        histogramData?.let { data ->
            for (i in 0 until 12) {
                val startIndex = i * 4
                val endIndex = min((i + 1) * 4, data.size)
                if (startIndex < endIndex) {
                    val monthlyAve = data.subList(startIndex, endIndex).average().toFloat()
                    entries.add(BarEntry(i.toFloat(), monthlyAve))
                }
            }
        }

        // Populate and style the bar chart if there are entries.
        if (entries.isNotEmpty()) {
            barChart.visibility = View.VISIBLE
            val barDataSet = BarDataSet(entries, "Frequency of Species")
            barDataSet.color = Color.GRAY
            barDataSet.setDrawValues(false)

            val barData = BarData(barDataSet)
            barChart.data = barData

            val currentMonth = Calendar.getInstance().get(Calendar.MONTH).toFloat()
            val limitLine = LimitLine(currentMonth)
            limitLine.lineWidth = 1f
            limitLine.lineColor = Color.BLACK
            limitLine.textColor = Color.BLACK
            limitLine.textSize = 12f
            limitLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP

            barChart.setExtraOffsets(0f, 0f, 0f, 0f)
            val xAxis = barChart.xAxis
            xAxis.removeAllLimitLines()
            xAxis.addLimitLine(limitLine)

            val yAxisLeft = barChart.axisLeft
            val yAxisRight = barChart.axisRight
            yAxisLeft.axisMaximum = 0.025f
            yAxisRight.axisMaximum = 0.025f
            yAxisLeft.setDrawLabels(false)
            yAxisRight.setDrawLabels(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = MonthValueFormatter()

            yAxisLeft.setDrawGridLines(false)
            yAxisRight.setDrawGridLines(false)

            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = MonthValueFormatter()
            xAxis.setLabelCount(12, false)
            xAxis.setGranularity(1f)

            barChart.invalidate()
        } else {
            barChart.visibility = View.GONE
        }
    }
}
