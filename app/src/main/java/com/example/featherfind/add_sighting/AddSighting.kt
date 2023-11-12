package com.example.featherfind.add_sighting

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.featherfind.MainActivity
import com.example.featherfind.R
import com.example.featherfind.databinding.FragmentAddSightingBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddSighting : Fragment() {

    companion object {
        fun newInstance(longitude: Double, latitude: Double): AddSighting {
            val fragment = AddSighting()
            val args = Bundle().apply {
                putDouble("longitude", longitude)
                putDouble("latitude", latitude)
            }
            fragment.arguments = args
            return fragment
        }        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


    private lateinit var viewModel: AddSightingViewModel
    private lateinit var binding: FragmentAddSightingBinding

    private val CAMERA_PERMISSION_REQUEST = 100
    private var IMAGE_PICKER_REQUEST = false
    private var CAMERA_REQUEST = false

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var hotspotLongitude: Double? = null
    private var hotspotLatitude: Double? = null
    private var currentPhotoRef: String? = null
    private fun requestLocationPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        hotspotLongitude = arguments?.getDouble("longitude")
        hotspotLatitude = arguments?.getDouble("latitude")
        val mainActivity = activity as? MainActivity

        //Used to get the user to add a photo to the entry
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (IMAGE_PICKER_REQUEST) {
                    val imageUri = data?.data
                    if (imageUri != null) {
                        viewModel.uploadPhoto(imageUri, requireContext()) { photoRef ->
                            // Update the photo reference here
                            currentPhotoRef = photoRef
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error selecting image", Toast.LENGTH_SHORT).show()
                    }
                    IMAGE_PICKER_REQUEST = false
                } else if (CAMERA_REQUEST) {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        viewModel.uploadPhoto(imageBitmap, requireContext()) { photoRef ->
                            // Update the photo reference here
                            currentPhotoRef = photoRef
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error capturing image", Toast.LENGTH_SHORT).show()
                    }
                    CAMERA_REQUEST = false
                }
            }
        }



        binding = FragmentAddSightingBinding.inflate(layoutInflater)

        // Get references to the EditText fields and the buttons
        val txtBirdName = binding.txtBirdName
        val txtBirdSpecies = binding.txtBirdSpecies
        val sightingDate = binding.datePicker
        val sightingTime = binding.sightingTimePicker
        val btnCurrentTime = binding.btnCurrentSightingTime
        val txtSightingDescription = binding.txtSightingDescription
        val btnAddPhoto = binding.btnAddPhoto
        val btnAddSighting = binding.btnAddSighting
        val mainScrollView = binding.mainScrollView

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

        //When the user clicks on the date option
        sightingDate.setOnClickListener() {
            val context = context ?: return@setOnClickListener // Use fragment's context
            //Displays a date picker to the user
            val datePickerListener =
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    // Update the button text with the selected date
                    val selectedDate = "$year-${monthOfYear + 1}-$dayOfMonth"
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateObj = inputFormat.parse(selectedDate)
                    val formattedDate = outputFormat.format(dateObj)
                    sightingDate.text = formattedDate
                }

            // Create a Calendar instance to set the initial date in the date picker
            val calendar = Calendar.getInstance()

            // Create a DatePickerDialog with the current date as the initial selection
            val datePickerDialog =
                DatePickerDialog(
                    context,
                    datePickerListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            // Show the date picker dialog
            datePickerDialog?.show()
        }

        //When the user clicks the start time button drop down
        sightingTime.setOnClickListener() {
            val context = context ?: return@setOnClickListener // Use fragment's context
            val popupMenu = PopupMenu(context, sightingTime)

            //Adds all the time options
            for (timeOption in timeOptions) {
                popupMenu.menu.add(timeOption)
            }

            //When the user selects a time option
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedTime = menuItem.title.toString()
                sightingTime.text = selectedTime
                true
            }
            //Shows the menu to the user
            popupMenu.show()
        }

        //When the user clicks the current time button under the start time
        btnCurrentTime.setOnClickListener {
            //Gets the current time
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            //Assigns the current time to the startTime
            sightingTime.text = current.format(formatter).toString()
        }

        //When the user clicks the add sighting button
        btnAddSighting.setOnClickListener() {
            if (txtBirdName.text.isNotEmpty() && txtBirdSpecies.text.isNotEmpty() && sightingDate.text != "Select Date"
                && sightingTime.text != "Select Time" && txtSightingDescription.text.isNotEmpty()) {

                // Check for location permissions before adding a sighting
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Request location permissions if not granted
                    requestLocationPermissions()
                } else {
                    // Permissions already granted, proceed with adding sighting
                    addSightingAtLocation()
                }
            } else {
                // User missed a field
                Toast.makeText(
                    requireContext(),
                    "Please make sure that you've filled in all the fields.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        //When the user clicks the add photo button
        btnAddPhoto.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), btnAddPhoto)
            popupMenu.menuInflater.inflate(R.menu.menu_photo_options, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_select_photo -> {
                        IMAGE_PICKER_REQUEST = true
                        // Launch the image picker
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "image/*"
                        imagePickerLauncher.launch(intent)
                        true
                    }
                    R.id.menu_take_photo -> {
                        // Check if the camera permission is granted
                        val hasCameraPermission = ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        // Request the camera permission if not granted
                        if (!hasCameraPermission) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(Manifest.permission.CAMERA),
                                CAMERA_PERMISSION_REQUEST
                            )
                            return@setOnMenuItemClickListener true // Return true to indicate the action was handled
                        }

                        // Launch the camera activity if permission is granted
                        CAMERA_REQUEST = true
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        imagePickerLauncher.launch(intent)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddSightingViewModel::class.java)

        val hotspotLongitude = arguments?.getDouble("longitude", 0.0) ?: 0.0
        val hotspotLatitude = arguments?.getDouble("latitude", 0.0) ?: 0.0

        context?.let { ctx ->
            viewModel.initLocationService(ctx)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissions granted, proceed with adding sighting
                    addSightingAtLocation()
                } else {
                    // Permissions denied, handle accordingly
                    Toast.makeText(requireContext(), "Location permission is required to add a sighting.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun addSightingAtLocation() {
        if (currentPhotoRef == null) {
            Toast.makeText(requireContext(), "No photo selected", Toast.LENGTH_SHORT).show()
            return
        }

        if (hotspotLatitude == null || hotspotLongitude == null) {
            // Hotspot not selected, use current location
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissions()
            } else {
                viewModel.addSightingWithCurrentLocation(
                    requireContext(),
                    binding.txtBirdName.text.toString(),
                    binding.txtBirdSpecies.text.toString(),
                    binding.datePicker.text.toString(),
                    binding.sightingTimePicker.text.toString(),
                    binding.txtSightingDescription.text.toString(),
                    currentPhotoRef!!, // Pass the photo reference here
                    {
                        Toast.makeText(requireContext(), "Sighting added successfully!", Toast.LENGTH_SHORT).show()
                        resetSightingForm()
                    },
                    { e ->
                        Toast.makeText(requireContext(), "Error adding sighting: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            // Hotspot selected, use provided coordinates
            viewModel.addSightingWithLocation(
                requireContext(),
                binding.txtBirdName.text.toString(),
                binding.txtBirdSpecies.text.toString(),
                binding.datePicker.text.toString(),
                binding.sightingTimePicker.text.toString(),
                binding.txtSightingDescription.text.toString(),
                hotspotLatitude!!,
                hotspotLongitude!!,
                currentPhotoRef!!, // Pass the photo reference here
                {
                    Toast.makeText(requireContext(), "Sighting added successfully!", Toast.LENGTH_SHORT).show()
                    resetSightingForm()
                },
                { e ->
                    Toast.makeText(requireContext(), "Error adding sighting: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }


    private fun resetSightingForm() {
        binding.txtBirdName.text = null
        binding.txtBirdSpecies.text = null
        binding.datePicker.text = "Select Date"
        binding.sightingTimePicker.text = "Select Time"
        binding.txtSightingDescription.text = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddSightingViewModel::class.java)
        // TODO: Use the ViewModel
    }
}

