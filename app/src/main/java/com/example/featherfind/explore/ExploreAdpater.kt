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
 * Adapter class for managing bird data in a RecyclerView.
 */
class BirdAdapter : RecyclerView.Adapter<BirdAdapter.BirdViewHolder>() {
    // List to hold the bird data
    private var birdList: List<Bird> = mutableListOf()

    /**
     * ViewHolder class to hold the UI elements for each bird item.
     */
    class BirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdNameTextView: TextView = itemView.findViewById(R.id.birdNameTextView)
        val birdSciNameTextView: TextView = itemView.findViewById(R.id.birdSciName)
        val barChartContainer: BarChart = itemView.findViewById(R.id.barChart)
    }

    /**
     * Inflates a new view and returns a new ViewHolder to hold it.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirdViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.bird_item, parent, false)
        return BirdViewHolder(itemView)
    }

    /**
     * Populates the data for each bird item.
     */
    override fun onBindViewHolder(holder: BirdViewHolder, position: Int) {
        val currentBird = birdList[position]
        holder.birdNameTextView.text = currentBird.comName
        holder.birdSciNameTextView.text = currentBird.sciName

        val barChart = holder.barChartContainer
        barChart.clear()  // Clear old data
        barChart.description.isEnabled = false
        val entries = mutableListOf<BarEntry>()

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
            holder.barChartContainer.visibility = View.GONE
        }
    }

    /**
     * Returns the number of items in the bird list.
     */
    override fun getItemCount(): Int {
        return birdList.size
    }

    /**
     * Updates the bird data and sorts it by the last word in the common name.
     *
     * @param newBirdList A new list of birds.
     */
    fun updateData(newBirdList: List<Bird>) {
        birdList = newBirdList.sortedBy {
            it.comName.split(" ").last()
        }
        notifyDataSetChanged()
    }

}
