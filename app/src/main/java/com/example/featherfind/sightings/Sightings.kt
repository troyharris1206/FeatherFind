package com.example.featherfind.sightings

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.featherfind.R
import java.util.Calendar
import android.widget.Button
import android.widget.TextView
import android.app.DatePickerDialog

class Sightings : Fragment() {

    companion object {
        fun newInstance() = Sightings()
    }

    private lateinit var viewModel: SightingsViewModel

    // Add the variables for the button and text view
    private lateinit var fromDate: Button
    private lateinit var toDate: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sightings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SightingsViewModel::class.java)
        // TODO: Use the ViewModel

        // Initialize your button and text view
        fromDate = requireView().findViewById(R.id.btnFromDate)
        toDate = requireView().findViewById(R.id.btnToDate)

        // Add a click listener to your button
        fromDate.setOnClickListener {
            // Add the code for the date picker dialog here
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view, year, monthOfYear, dayOfMonth ->
                    fromDate.text =
                        (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

        // Add a click listener to your button
        toDate.setOnClickListener {
            // Add the code for the date picker dialog here
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view, year, monthOfYear, dayOfMonth ->
                    toDate.text =
                        (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }
    }

}