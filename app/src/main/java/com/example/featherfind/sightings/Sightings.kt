package com.example.featherfind.sightings

import android.annotation.SuppressLint
import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.featherfind.R
import java.util.Calendar
import android.widget.Button
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.featherfind.Birds
import com.example.featherfind.BirdsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.featherfind.BirdsStatisticsAdapter
import com.example.featherfind.MainActivity
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class Sightings : Fragment() {

    companion object {
        fun newInstance() = Sightings()
    }

    private lateinit var viewModel: SightingsViewModel

    // Add the variables for the button and text view
    private lateinit var fromDate: Button
    private lateinit var toDate: Button
    private lateinit var viewAll: Button
    private lateinit var searchBird: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var editSightings: Button
    private lateinit var viewStatistics: Button

    //onCreateView method header
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sightings, container, false)
    }

    //onActivityCreated method header
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SightingsViewModel::class.java)
        // TODO: Use the ViewModel

        var fromCalendar: Calendar?= null
        var toCalendar: Calendar?= null

        // Initialize your button and text view
        fromDate = requireView().findViewById(R.id.btnFromDate)
        toDate = requireView().findViewById(R.id.btnToDate)
        searchBird = requireView().findViewById(R.id.btnFilterByDates)
        viewAll = requireView().findViewById(R.id.btnViewAllSightings)
        editSightings = requireView().findViewById(R.id.btnEditSightings)
        viewStatistics = requireView().findViewById(R.id.btnViewStatistics)

        //viewstatistics functionality
        viewStatistics.setOnClickListener {
            showPopupStatistics(requireContext())//calls pop up
        }

        // FromDate datepicker functionality
        fromDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    fromCalendar = Calendar.getInstance()
                    fromCalendar?.set(year, monthOfYear, dayOfMonth)
                    // Format the day with a leading zero if it's below 10
                    val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
                    fromDate.text = "$year-${monthOfYear + 1}-$formattedDay"
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

        // toDate datepicker functionality
        toDate.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    toCalendar = Calendar.getInstance()
                    toCalendar?.set(year, monthOfYear, dayOfMonth)
                    // Format the day with a leading zero if it's below 10
                    val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
                    toDate.text = "$year-${monthOfYear + 1}-$formattedDay"
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

        //SearchBird button functionality checks for birds sighted in between the date range
        searchBird.setOnClickListener {
            val db = Firebase.firestore
            val birdsCollection = db.collection("Sightings")

            val userUID = FirebaseAuth.getInstance().currentUser?.uid

            //If statement for date range validation
            if (fromCalendar != null && toCalendar != null && !toCalendar!!.before(fromCalendar)) {
                val fromDateText = fromDate.text.toString()
                val toDateText = toDate.text.toString()

                // Create a query to filter by userUID and date range
                val query = birdsCollection
                    .whereEqualTo("userUID", userUID)
                    .whereGreaterThanOrEqualTo("dateOfSighting", fromDateText)
                    .whereLessThanOrEqualTo("dateOfSighting", toDateText)

                query.get()
                    .addOnSuccessListener { documents ->
                        val birds = ArrayList<Birds>()
                        for (document in documents) {
                            val bird = document.toObject(Birds::class.java)
                            birds.add(bird)
                        }

                        //sets findings to the recycler view
                        recyclerView = requireView().findViewById(R.id.birdRecyclerView)
                        recyclerView.layoutManager = LinearLayoutManager(context)
                        val adapter = BirdsAdapter(birds)
                        recyclerView.adapter = adapter
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting documents: ", exception)
                    }
            } else {
                Toast.makeText(requireContext(), "Invalid date range", Toast.LENGTH_SHORT).show()
            }
        }

        //viewAllSightings button functionality, gets all birds sighting associated with userID
        viewAll.setOnClickListener {
            val db = Firebase.firestore
            val birdsCollection = db.collection("Sightings")

            // Replace "attributeName" and "value" with the actual attribute name and value to filter by
            val attributeName = "userUID"
            val value = FirebaseAuth.getInstance().currentUser?.uid

            // Create a query to filter the documents
            val query = birdsCollection.whereEqualTo(attributeName, value)

            query.get()
                .addOnSuccessListener { documents ->
                    val birds = ArrayList<Birds>()
                    for (document in documents) {
                        val bird = document.toObject(Birds::class.java)
                        birds.add(bird)
                    }

                    //sets findings to the recycler view
                    recyclerView = requireView().findViewById(R.id.birdRecyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    val adapter = BirdsAdapter(birds)
                    recyclerView.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }

        }

        //editSightings button functionality
        editSightings.setOnClickListener {
            showPopupEdit(requireContext())//calls pop up
        }
    }


    @SuppressLint("MissingInflatedId")
    fun showPopupStatistics(context: Context) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_statistics_bird_layout, null)

        builder.setView(view)
        val dialog = builder.create()

        // Initialize your Spinner
        val birdSpinner = view.findViewById<Spinner>(R.id.yearSpinner)

        // Create an array of years from 2021 to 2030
        val years = (2021..2030).map { it.toString() }.toTypedArray()

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, years)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        birdSpinner.adapter = adapter

        // Set an OnItemSelectedListener to update the query when a year is selected
        birdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                // Get the selected year from the spinner
                val selectedYear = years[position].toInt()

                val db = Firebase.firestore
                val birdsCollection = db.collection("Sightings")


                // Replace "attributeName" and "value" with the actual attribute name and value to filter by
                val attributeName = "userUID"
                val value = FirebaseAuth.getInstance().currentUser?.uid

                // Create a query to filter the documents based on the extracted year
                val query = birdsCollection
                    .whereEqualTo(attributeName, value)

                query.get()
                    .addOnSuccessListener { documents ->
                        val birdsMap = HashMap<String, Int>() // Map to store sightings count for each month

                        for (document in documents) {
                            val bird = document.toObject(Birds::class.java)
                            val year = extractYearFromDateString(bird.dateOfSighting)

                            if (year == selectedYear) {
                                // If the year matches the selected year, proceed to count sightings for each month
                                val month = extractMonthFromDateString(bird.dateOfSighting)

                                // Update the count for the month in the map
                                birdsMap[month] = (birdsMap[month] ?: 0) + 1
                            }
                        }

                        // Convert the map to a list of pairs for the adapter
                        val birdStatisticsList = birdsMap.entries.map { (month, count) -> month to count }

                        // Set the findings to the RecyclerView
                        recyclerView = view.findViewById(R.id.statisticsRecyclerView)
                        recyclerView.layoutManager = LinearLayoutManager(context)
                        val adapter = BirdsStatisticsAdapter(birdStatisticsList)
                        recyclerView.adapter = adapter
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "Error getting documents: ", exception)
                    }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Handle no selection here
            }
        }

        dialog.show()
    }

    private fun extractYearFromDateString(dateString: String): Int {
        // Assuming dateString is in the format "yyyy-MM-dd"
        val parts = dateString.split("-")
        return if (parts.size == 3) {
            // Year is at index 0 (0-based indexing)
            parts[0].toIntOrNull() ?: 0
        } else {
            // Handle invalid date format
            0
        }
    }

    private fun extractMonthFromDateString(dateString: String): String {
        // Assuming dateString is in the format "yyyy-MM-dd"
        val parts = dateString.split("-")
        return if (parts.size == 3) {
            // Month is at index 1 (0-based indexing)
            parts[1]
        } else {
            // Handle invalid date format
            ""
        }
    }


    //Pop up for edit sightings
    @RequiresApi(Build.VERSION_CODES.O)
    fun showPopupEdit(context: Context) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_edit_bird_layout, null)

        builder.setView(view)
        val dialog = builder.create()

        //Gets the time options for the user to pick from in the dropdown
        val timeOptions = Array(48) { "" }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        //Gets the calendar for the user to select from
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)

        //Assigns the time options to an array
        for (i in 0 until 48) {
            val time = timeFormat.format(calendar.time)
            timeOptions[i] = time
            calendar.add(Calendar.MINUTE, 30)
        }

        // Initialize your Spinner
        val birdSpinner = view.findViewById<Spinner>(R.id.birdSpinner) // Added the explicit type declaration
        val btnCurrentTime = view.findViewById<Button>(R.id.btnCurrentSightingTime)
        val sightingTimePicker = view.findViewById<Button>(R.id.sightingTimePicker)

        //When the user clicks the start time button drop down
        sightingTimePicker.setOnClickListener() {
            val mainActivity = activity as? MainActivity
            val popupMenu = PopupMenu(mainActivity, sightingTimePicker)

            //Adds all the time options
            for (timeOption in timeOptions) {
                popupMenu.menu.add(timeOption)
            }

            //When the user selects a time option
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedTime = menuItem.title.toString()
                sightingTimePicker.text = selectedTime
                true
            }
            //Shows the menu to the user
            popupMenu.show()
        }

        // Get references to other UI elements you want to populate
        val birdName = view.findViewById<EditText>(R.id.txtBirdName)
        val birdSpecie = view.findViewById<EditText>(R.id.txtBirdSpecies)
        val birdDate = view.findViewById<Button>(R.id.datePicker)

        //When the user clicks on the date option
        birdDate.setOnClickListener() {
            val mainActivity = activity as? MainActivity

            //Displays a date picker to the user
            val datePickerListener =
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    // Update the button text with the selected date
                    val selectedDate = "$year-${monthOfYear + 1}-$dayOfMonth"
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateObj = inputFormat.parse(selectedDate)
                    val formattedDate = outputFormat.format(dateObj)
                    birdDate.text = formattedDate
                }

            // Create a Calendar instance to set the initial date in the date picker
            val calendar = Calendar.getInstance()

            // Create a DatePickerDialog with the current date as the initial selection
            val datePickerDialog = mainActivity?.let { main ->
                DatePickerDialog(
                    main,
                    datePickerListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }

            // Show the date picker dialog
            datePickerDialog?.show()
        }

        val birdTime = view.findViewById<Button>(R.id.sightingTimePicker)
        val birdDescription= view.findViewById<EditText>(R.id.txtSightingDescription)
        var birdDataList: List<Birds> = emptyList()

        //When the user clicks the current time button under the start time
        btnCurrentTime.setOnClickListener {
            //Gets the current time
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            //Assigns the current time to the startTime
            sightingTimePicker.text = current.format(formatter).toString()
        }

        // Get the current user's UID
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is authenticated
        if (currentUser != null) {
            // Initialize Firestore
            val db = FirebaseFirestore.getInstance()
            val birdsCollection = db.collection("Sightings")

            // Query Firestore for bird names with matching userUID
            birdsCollection
                .whereEqualTo("userUID", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    birdDataList = documents.documents.mapNotNull { document ->
                        document.getDouble("latitude")?.let {
                            Birds(
                                document.getString("birdName").toString(),
                                document.getString("birdSpecies").toString(),
                                document.getString("dateOfSighting").toString(),
                                it,
                                it,
                                document.getString("photoReference").toString(),
                                document.getString("sightingDescription").toString(),
                                document.getString("timeOfSighting").toString(),
                                document.getString("userUID").toString()
                            )
                        }
                    }

                    //Array adapter for the spinner component
                    val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, birdDataList.map { it.birdName })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    birdSpinner.adapter = adapter

                    //Gets all the variables for the selected bird
                    birdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selectedBird = birdDataList[position]
                            birdName.setText(selectedBird.birdName)
                            birdSpecie.setText(selectedBird.birdSpecies)
                            birdDate.setText(selectedBird.dateOfSighting)
                            birdTime.setText(selectedBird.timeOfSighting)
                            birdDescription.setText(selectedBird.sightingDescription)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Handle when nothing is selected in the spinner
                            // There are no options that are null
                        }
                    }
                }
        }

        //ConfirmChanges button functionality, to update all the changes made to the sighting
        val confirmChanges = view.findViewById<Button>(R.id.btnUpdateSighting)

        confirmChanges.setOnClickListener {
            // Get the selected bird from the spinner
            val selectedPosition = birdSpinner.selectedItemPosition
            val selectedBird = birdDataList[selectedPosition]

            // Get the updated values from the EditText fields
            val updatedName = birdName.text.toString()
            val updatedSpecies = birdSpecie.text.toString()
            val updatedDate = birdDate.text.toString()
            val updatedTime = birdTime.text.toString()
            val updatedDescription = birdDescription.text.toString()

            if (updatedName.isNotEmpty() && updatedSpecies.isNotEmpty() && updatedDate.isNotEmpty() && updatedTime.isNotEmpty() && updatedDescription.isNotEmpty()) {
                // Update the selected bird's Firestore document with the new values
                val db = FirebaseFirestore.getInstance()
                val birdsCollection = db.collection("Sightings")

                //query to find the document with the specific name and userUID
                val query = birdsCollection
                    .whereEqualTo("birdName", selectedBird.birdName)
                    .whereEqualTo("userUID", FirebaseAuth.getInstance().currentUser?.uid)

                query.get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            // Handle the case where no matching document is found
                            Toast.makeText(context, "Bird sighting not found for update.", Toast.LENGTH_SHORT).show()
                        } else {
                            for (document in documents) {
                                document.reference.update(
                                    "birdName", updatedName,
                                    "birdSpecies", updatedSpecies,
                                    "dateOfSighting", updatedDate,
                                    "timeOfSighting", updatedTime,
                                    "sightingDescription", updatedDescription
                                ).addOnSuccessListener {
                                    // Handle the update success
                                    Toast.makeText(context, "Bird sighting updated successfully.", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss() // Close the dialog after the update
                                }.addOnFailureListener { exception ->
                                    // Handle the update failure
                                    Toast.makeText(context, "Error updating bird sighting: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Handle the query failure
                        Toast.makeText(context, "Error querying bird sighting: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }else{
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        //Cancel button functionality, for if the user wishes to not edit the sighting
        val cancelButton = view.findViewById<Button>(R.id.btnCancelUpdateSighting)

        cancelButton.setOnClickListener {

            dialog.dismiss()
        }
        dialog.show()
    }
}