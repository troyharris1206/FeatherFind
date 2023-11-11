package com.example.featherfind.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.featherfind.R
import com.example.featherfind.add_sighting.AddSighting
import com.example.featherfind.databinding.ActivityMapsBinding
import com.example.featherfind.explore.BirdRepository.getGoogleDirections
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.maps.android.PolyUtil
import org.json.JSONException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback

/**
 * MapsActivity: Activity to display Google Maps and hotspots.
 * Implements OnMapReadyCallback to get notified when the map is ready.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // Member Variables
    private lateinit var mMap: GoogleMap  // Google Map object
    private lateinit var binding: ActivityMapsBinding  // View binding object
    private lateinit var fusedLocationClient: FusedLocationProviderClient  // Location client
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002  // Request code for location permissions
    private lateinit var viewModel: HotspotViewModel  // ViewModel for hotspots
    private var maxDistance: Float = 1f  // Max distance to filter hotspots (in meters)
    private var userLocation: LatLng = LatLng(0.0, 0.0)  // User's location
    private var allHotspots: List<Hotspot> = listOf()  // List to store all hotspots
    private var travelDistance: String = ""  // Travel distance
    private var travelTime: String = ""  // Travel time
    private var directionSteps: ArrayList<String> = ArrayList()  // List to hold direction steps
    private var currentPolyline: Polyline? = null  // Polyline object to represent the route on the map
    private var selectedHotspotName: String? = null
    private var behavior: BottomSheetBehavior<View>? = null
    private var originalPeekHeight: Int = 0
    private val seekBarHandler = Handler(Looper.getMainLooper())
    private val routePolylines = mutableListOf<Polyline>()
    private var selectedRouteIndex = 0  // Default to the first route
    // This will hold the original JSON response so we can redraw routes when a new one is selected.
    private var lastDirectionsJson: String? = null
    // Class-level variable to store references to all markers
    private val allMarkers = mutableListOf<Marker>()
    private var userSightings: MutableList<UserSighting> = mutableListOf()
    private val sightingsByLocation: MutableMap<LatLng, MutableList<UserSighting>> = mutableMapOf()

    // Class-level variable to store the selected marker
    private var selectedMarker: Marker? = null
    private val userSightingMarkers = mutableListOf<Marker>()

    /**
     * Called when the activity is created.
     * Initializes the map, ViewModel, and other UI components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchUserSightingsOnInit()

        // Initialize View Binding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val backButton: Button = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        // Initialize Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize ViewModel and Fused Location Client
        viewModel = ViewModelProvider(this).get(HotspotViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Observe changes in the hotspot list
        viewModel.hotspotList.observe(this) { hotspots ->
            allHotspots = hotspots ?: listOf()
            filterHotspotsByDistance()
        }

        onBackPressedDispatcher.addCallback(this) {
                finish()
        }

        val sightingsSwitch: SwitchMaterial = findViewById(R.id.sightings)
        sightingsSwitch.isChecked = false
        sightingsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if(::mMap.isInitialized) {
                updateMapMarkersBasedOnSwitch(isChecked)
            }
        }
        // Request the user's current location
        requestUserLocation()
        // Initialize SeekBar and TextView for maximum value
        val distanceSeekBar: SeekBar = findViewById(R.id.distanceSeekBar)
        val maxValueTextView: TextView = findViewById(R.id.maxValue)

        // Fetch the current authenticated user
        val currentUser = auth.currentUser

        // Check if the user is logged in
        if (currentUser != null) {
            val userDocument = db.collection("Users").document(currentUser.uid)

            userDocument.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val storedMaxDistance = document.getString("maxDistance")?.toFloatOrNull()
                        if (storedMaxDistance != null) {
                            maxDistance = storedMaxDistance
                            Log.d("Firestore", "$savedInstanceState")
                            distanceSeekBar.max = (storedMaxDistance).toInt() // Max distance in meters
                            distanceSeekBar.progress = (storedMaxDistance).toInt() // Initially set to max

                            // Set TextView to show max value
                            maxValueTextView.text = "Max: ${maxDistance.toInt()}"

                            // Call the filter function initially
                            filterHotspotsByDistance()

                        } else {
                            Log.d("Firestore", "maxDistance in DB is null")
                        }
                    } else {
                        Log.d("Firestore", "Document does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error getting document", e)
                }
        }

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Cancel the last runnable if it exists
                seekBarHandler.removeCallbacksAndMessages(null)

                // Create a new runnable
                val newRunnable = Runnable {
                    maxDistance = (progress.toFloat()) // You might need to adjust this based on your use-case
                    filterHotspotsByDistance()

                    // Update TextView to show the newly set max value
                    maxValueTextView.text = "Max: $progress"
                }

                // Execute the runnable after 300ms
                seekBarHandler.postDelayed(newRunnable, 300)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional
            }
        })
    }

    private fun updateMapMarkersBasedOnSwitch(showSightings: Boolean) {
        userSightingMarkers.forEach { marker ->
            marker.isVisible = showSightings
        }
        allMarkers.forEach { marker ->
            if (!userSightingMarkers.contains(marker)) {
                marker.isVisible = !showSightings
            }
        }
    }

    /**
     * Filters the list of hotspots based on their distance from the user's current location.
     * Only hotspots within 'maxDistance' meters are included.
     * After filtering, updates the map markers to reflect the filtered list.
     */
    private fun filterHotspotsByDistance() {
        val maxDistanceInMeters = maxDistance * 1000
        Log.d("Firestore", "$maxDistanceInMeters")
        val filteredHotspots = allHotspots.filter { hotspot ->
            val hotspotLocation = LatLng(hotspot.longitude, hotspot.latitude)
            val distance = distanceBetween(userLocation, hotspotLocation)
            distance <= maxDistanceInMeters
        }
        val sightingsSwitch: SwitchMaterial = findViewById(R.id.sightings)
        updateMapMarkersBasedOnSwitch(sightingsSwitch.isChecked)
        updateMapMarkers(filteredHotspots, userSightings)
        Log.d("Debug", "Filtered Hotspots: $filteredHotspots")
    }
    /**
     * Calculates the distance between two LatLng points using Android's Location API.
     * @param point1 The first geographical point.
     * @param point2 The second geographical point.
     * @return The distance between point1 and point2 in meters.
     */
    private fun distanceBetween(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)  // Array to hold the distance result

        // Calculate distance between two points
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]  // Return the calculated distance
    }

    /**
     * Draws the driving route on the map and shows direction steps, distance, and time.
     * The function parses a JSON response from a mapping service to retrieve route details.
     * @param directions The JSON string containing route information.
     */
    // Class-level variables


    private fun drawRoute(directions: String) {
        lastDirectionsJson = directions // Store the JSON response for later use.
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(directions)
            val routesArray = jsonResponse.getJSONArray("routes")

            // Clear previous direction steps and polylines to prepare for new directions
            directionSteps.clear()
            routePolylines.forEach { it.remove() } // Remove all polylines from the map
            routePolylines.clear() // Clear the list of polyline references

            // Iterate over all routes
            for (r in 0 until routesArray.length()) {
                val route = routesArray.getJSONObject(r)
                val legs = route.getJSONArray("legs")

                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)

                    // Draw the polyline for each route
                    val poly = route.getJSONObject("overview_polyline")
                    val polyline = poly.getString("points")
                    val decodedPath = PolyUtil.decode(polyline)
                    val polylineOptions = PolylineOptions()
                        .addAll(decodedPath)
                        .color(if (r == selectedRouteIndex) Color.rgb(249, 142, 85) else Color.GRAY)
                        .clickable(true)
                        .zIndex(if (r == selectedRouteIndex) 1f else 0f) // Set zIndex based on selection
                    val polylineObject = mMap.addPolyline(polylineOptions)
                    polylineObject.tag = r
                    routePolylines.add(polylineObject)

                    if (r == selectedRouteIndex) {
                        displaySelectedRouteDirections(r)
                        selectedHotspotName?.let { showDirectionSteps(it) } // This will update the UI with the steps
                    }
                }
            }

            // Adjust the camera view to include all route points from the selected route
            adjustCameraToRoute(routesArray.getJSONObject(selectedRouteIndex))

            // Set click listener for polylines outside the loop
            mMap.setOnPolylineClickListener { polyline ->
                val index = polyline.tag as? Int ?: return@setOnPolylineClickListener
                onRouteSelected(index)
            }

        } catch (e: JSONException) {
            Log.e("MapsActivity", "JSON parsing error: ${e.message}")
        }
    }

    // Call this method when an alternative route is selected
    private fun onRouteSelected(routeIndex: Int) {
        selectedRouteIndex = routeIndex
        // Parse the JSON response stored in lastDirectionsJson
        val jsonResponse = JSONObject(lastDirectionsJson)
        val routesArray = jsonResponse.getJSONArray("routes")

        // Update the zIndex for all polylines
        routePolylines.forEachIndexed { index, polyline ->
            polyline.zIndex = if (index == selectedRouteIndex) 1f else 0f
            polyline.color = if (index == selectedRouteIndex) Color.rgb(249, 142, 85) else Color.GRAY
        }

        // Redraw the directions and adjust the map
        displaySelectedRouteDirections(routeIndex)
        adjustCameraToRoute(routesArray.getJSONObject(routeIndex))

        // Ensure that the selected hotspot name is set and call showDirectionSteps
        if (selectedHotspotName != null) {
            showDirectionSteps(selectedHotspotName!!)
        }
    }
    // This function adjusts the camera to the selected route
    private fun adjustCameraToRoute(selectedRoute: JSONObject) {
        val poly = selectedRoute.getJSONObject("overview_polyline")
        val polyline = poly.getString("points")
        val decodedPath = PolyUtil.decode(polyline)
        val boundsBuilder = LatLngBounds.Builder()
        for (point in decodedPath) {
            boundsBuilder.include(point)
        }
        val bounds = boundsBuilder.build()
        val padding = 250
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.animateCamera(cu)
    }

    // This function displays the direction steps for the selected route
    private fun displaySelectedRouteDirections(routeIndex: Int) {
        // Assuming `lastDirectionsJson` is not null and contains the JSON response with all routes
        val jsonResponse = JSONObject(lastDirectionsJson)
        val routesArray = jsonResponse.getJSONArray("routes")
        val route = routesArray.getJSONObject(routeIndex)
        val legs = route.getJSONArray("legs")

        if (legs.length() > 0) {
            val leg = legs.getJSONObject(0)

            travelDistance = leg.getJSONObject("distance").getString("text")
            travelTime = leg.getJSONObject("duration").getString("text")
            directionSteps.clear() // Clear existing steps

            val steps = leg.getJSONArray("steps")
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                var instruction = step.getString("html_instructions")
                instruction = Html.fromHtml(instruction).toString()
                directionSteps.add(instruction)
            }
        }
    }


    /**
     * This function displays direction steps in a bottom sheet dialog.
     *
     * @param hotspotName The name of the hotspot for which directions are displayed.
     */
    private fun showDirectionSteps(hotspotName: String) {
        // Initialize Firebase Auth and Firestore
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Fetch the current user
        val currentUser = auth.currentUser

        // Initialize bottom sheet dialog and its view
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)

        // Find and store the selected marker
        selectedMarker = allMarkers.firstOrNull { it.tag == hotspotName }

        // Fetch user preference for distance unit (either "miles" or "kilometers")
        firestore.collection("Users").document(currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val preferredUnit = document.getString("measurementSystem") ?: "Metric"

                // Extract numerical part from the string (assuming the format is always like "4.2 km")
                val numericalPart = travelDistance.split(" ")[0]

                // Convert distance to miles if necessary
                val displayedTravelDistance = if (preferredUnit == "Imperial") {
                    val miles = numericalPart.replace(",", "").toDouble() * 0.621371 // Conversion factor for km to miles
                    String.format("%.2f miles", miles)
                } else {
                    "$numericalPart km"
                }

                // Set up ListView adapter to show direction steps
                val arrayAdapter =
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, directionSteps)
                val listView = bottomSheetView.findViewById<ListView>(R.id.listView)
                listView.adapter = arrayAdapter

                // Display hotspot name
                val hotspotNameTextView = bottomSheetView.findViewById<TextView>(R.id.hotspot_name)
                hotspotNameTextView.text = "Hotspot: $hotspotName"

                // Display travel distance and time
                val travelDistanceTextView =
                    bottomSheetView.findViewById<TextView>(R.id.travel_distance)
                val travelTimeTextView = bottomSheetView.findViewById<TextView>(R.id.travel_time)
                travelDistanceTextView.text = "Distance: $displayedTravelDistance"
                travelTimeTextView.text = "Travel Time: $travelTime"

                // Set the bottom sheet dialog content
                bottomSheetDialog.setContentView(bottomSheetView)

                // Get the BottomSheetBehavior and configure its properties
                val bottomSheetInternal =
                    bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                behavior = BottomSheetBehavior.from(bottomSheetInternal!!)

                // Set initial height to 1/3 of the screen
                originalPeekHeight = resources.displayMetrics.heightPixels / 3
                behavior?.peekHeight = originalPeekHeight

                behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                // Set the height to the hotspot name height when collapsed
                                behavior?.peekHeight = hotspotNameTextView.height
                            }
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                // Restore the original peek height when expanded
                                behavior?.peekHeight = originalPeekHeight
                            }
                            else -> {}
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        if (slideOffset < 0) { // Indicates that the sheet is being swiped down
                            behavior?.peekHeight =
                                hotspotNameTextView.height // Set the height to the hotspot name height
                        }
                    }
                })
                bottomSheetDialog.setOnDismissListener {
                    // Call this when the bottom sheet is dismissed
                    closeDirectionSteps()
                }

                bottomSheetDialog.show()
            }
    }

    /**
     * Asynchronously fetches route directions from the Google Directions API.
     * Calls [drawRoute] to display the route on the map.
     *
     * @param context The application context
     * @param origin The starting point for the route
     * @param destination The destination point for the route
     */
    // When initially fetching directions, after getting the response:
    private suspend fun getDirections(context: Context, origin: LatLng, destination: LatLng, hotspotName: String) {
        val response = getGoogleDirections(context, origin, destination)
        if (response != null) {
            selectedHotspotName = hotspotName // Set the selected hotspot name
            drawRoute(response) // Draw the route and internally handle direction steps display
        } else {
            Log.e("MapsActivity", "Google Directions API returned null.")
        }
    }

    // Modify this to reset marker visibility when direction steps are closed
    private fun closeDirectionSteps() {
        updateMarkerVisibility(showAll = true)
    }

    /**
     * Called when the Google Map is ready to be used.
     * Configures initial settings for the map and sets listeners.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable UI controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Set location enabled if permissions are granted
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mMap.isMyLocationEnabled = true
        }

        // Fetch and display initial markers
        // Initially show only hotspot markers
        updateMapMarkers(allHotspots, userSightings)
        updateMapMarkersBasedOnSwitch(false) // Hide user sighting markers initially

        // Set up listener for the switch to toggle sighting markers
        val sightingsSwitch: SwitchMaterial = findViewById(R.id.sightings)
        sightingsSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateMapMarkersBasedOnSwitch(isChecked)
        }

        // Set marker and map click listeners
        mMap.setOnMarkerClickListener { marker ->
            onMarkerClick(marker)
            selectedHotspotName = marker.tag as? String ?: marker.title
            false
        }
        mMap.setOnMapClickListener {
            behavior?.apply {
                state = BottomSheetBehavior.STATE_COLLAPSED
                peekHeight = originalPeekHeight
            }
        }
    }


    /**
     * Clears existing markers and adds new ones for each hotspot.
     * Uses custom icons for the markers.
     *
     * @param hotspots List of hotspots to display on the map.
     */
    private fun updateMapMarkers(hotspots: List<Hotspot>, userSightings: List<UserSighting>) {
        mMap.clear()
        allMarkers.clear()
        userSightingMarkers.clear()

        val hotspotIcon = prepareMarkerIcon(R.drawable.baseline_location_on_24)
        hotspots.forEach { addMarker(it, hotspotIcon) }

        val sightingIcon = prepareMarkerIcon(R.drawable.baseline_location_on_25)
        sightingsByLocation.forEach { (location, sightings) ->
            addMarkerForSightings(location, sightings, sightingIcon, isVisible = false) // Initially invisible
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
    }

    private fun addMarkerForSightings(location: LatLng, sightings: List<UserSighting>, markerIcon: BitmapDescriptor, isVisible: Boolean) {
        val title = sightings.first().birdName
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(markerIcon)
                .visible(isVisible) // Set initial visibility
        )
        marker?.tag = sightings
        marker?.let {
            allMarkers.add(it)
            userSightingMarkers.add(it) // Add to the user sighting markers list
        }
    }

    // Call this function when a hotspot is selected or direction steps are closed
    private fun updateMarkerVisibility(showAll: Boolean) {
        for (marker in allMarkers) {
            marker.isVisible = if (showAll) {
                true // Show all markers if showAll is true
            } else {
                marker == selectedMarker // Only show the selected marker otherwise
            }
        }
    }


    private fun addMarker(location: Any, markerIcon: BitmapDescriptor) {
        val locationLatLng = when (location) {
            is Hotspot -> LatLng(location.longitude, location.latitude)
            is UserSighting -> LatLng(location.latitude, location.longitude)
            else -> return // Ignore other types
        }

        val title = when (location) {
            is Hotspot -> location.name
            is UserSighting -> location.birdName
            else -> "Unknown Location"
        }

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(locationLatLng)
                .title(title)
                .icon(markerIcon)
        )
        marker?.tag = location // Assign the entire object to the tag
        marker?.let { allMarkers.add(it) }
    }



    /**
     * Checks if the user has granted location permissions.
     *
     * @return True if either fine or coarse location permission is granted, otherwise false.
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the user's last known location and updates the map accordingly.
     * Also sets the region code in the ViewModel based on the location.
     */
    private fun requestUserLocation() {
        if (hasLocationPermission()) {
            // Check for permissions again, although redundant (required by the Android framework)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Use Kotlin Coroutines to fetch location asynchronously
            lifecycleScope.launch {
                try {
                    // Await the last known location
                    val location = fusedLocationClient.lastLocation.await()

                    if (location != null) {
                        // Update user's location and camera view
                        userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 10f))

                        // Determine the region code based on the location
                        val regionCode = determineRegionCode(this@MapsActivity, location)

                        // Update the ViewModel with the region code
                        viewModel.setRegionCode(regionCode)

                        // Fetch hotspots based on the updated region
                        viewModel.fetchHotspots()
                    }
                } catch (exception: Exception) {
                    Log.e("MapsActivity", "Failed to get user location: ${exception.message}")
                }
            }
        } else {
            // Request location permissions if not granted
            requestLocationPermissions()
        }
    }

    /**
     * Determines the region code of a given location using reverse geocoding.
     *
     * @param context The application context
     * @param location The location object containing latitude and longitude
     * @return The country code as the region code, or "unknown_region" if geocoding fails
     */
    private fun determineRegionCode(context: Context, location: Location): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            // Attempt to reverse geocode the location
            val addresses: List<Address>? =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                // Use the first address, generally the most accurate
                val address = addresses[0]

                // Return the country code as the region code
                return address.countryCode ?: "unknown_country"
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Default to "unknown_region" if geocoding fails
        return "unknown_region"
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Converts a Drawable to a Bitmap.
     *
     * @param drawable The Drawable object to be converted
     * @return A Bitmap representation of the provided Drawable
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        // If the Drawable is a BitmapDrawable, simply return its Bitmap
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // Create a Bitmap object with the dimensions of the Drawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // Prepare a Canvas to draw the Drawable on the Bitmap
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Handles the event when a map marker is clicked.
     *
     * @param marker The clicked Marker object
     * @return True to indicate that the click event has been consumed
     */
    private fun onMarkerClick(marker: Marker): Boolean {
        currentPolyline?.remove() // Remove existing polyline
        selectedMarker = marker // Assign the clicked marker to selectedMarker

        when (val selectedLocation = marker.tag) {
            is Hotspot -> {
                val hotspot = marker.tag as Hotspot
                showHotspotOptionsDialog(hotspot)
            }
            is List<*> -> {
                selectedLocation.filterIsInstance<UserSighting>().let { sightings ->
                    if (sightings.isNotEmpty()) {
                        displaySightingDetails(sightings)
                    }
                }
            }
        }
        return true
    }
    private fun showHotspotOptionsDialog(hotspot: Hotspot) {
        val options = arrayOf("Get Directions", "Add Sighting")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option for ${hotspot.name}")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> getDirectionsForHotspot(hotspot)
                1 -> openAddSightingFragment(hotspot)
            }
        }
        builder.show()
    }

    private fun getDirectionsForHotspot(hotspot: Hotspot) {
        val destination = LatLng(hotspot.longitude, hotspot.latitude)
        lifecycleScope.launch {
            getDirections(this@MapsActivity, userLocation, destination, hotspot.name)
        }
    }

    private fun openAddSightingFragment(hotspot: Hotspot) {
        val fragment = AddSighting.newInstance(hotspot.longitude, hotspot.latitude)
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, AddSighting.newInstance(hotspot.longitude, hotspot.latitude))
            .addToBackStack(null) // Add this transaction to the back stack
            .commit()
    }


    private fun displaySightingDetails(sightings: List<UserSighting>) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_sighting_layout, null)

        val sightingNameTextView = bottomSheetView.findViewById<TextView>(R.id.sighting_name)
        val sightingDetailsTextView = bottomSheetView.findViewById<TextView>(R.id.sighting_details)

        // Building the string for sighting names and details
        val sightingNames = "Sightings: " + sightings.joinToString(separator = ", ") { it.birdName }
        val sightingDetails = sightings.joinToString(separator = "\n\n") { sighting ->
            "Species: ${sighting.birdSpecies}\n" +
                    "Date: ${sighting.dateOfSighting}\n" +
                    "Time: ${sighting.timeOfSighting}\n" +
                    "Description: ${sighting.sightingDescription}"
        }

        sightingNameTextView.text = sightingNames
        sightingDetailsTextView.text = sightingDetails

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun fetchUserSightingsOnInit() {
        lifecycleScope.launch {
            Log.d("MapsActivity", "Fetching user sightings on init")
            val db = FirebaseFirestore.getInstance()
            val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserUID != null) {
                try {
                    val documents = db.collection("Sightings")
                        .whereEqualTo("userUID", currentUserUID)
                        .get()
                        .await()

                    if (documents.isEmpty) {
                        Log.d("MapsActivity", "No sightings found for user: $currentUserUID")
                    } else {
                        userSightings.clear()
                        sightingsByLocation.clear()
                        for (document in documents) {
                            val sighting = document.toObject(UserSighting::class.java)
                            if (sighting.latitude != 0.0 && sighting.longitude != 0.0) {
                                val sightingLatLng = LatLng(sighting.longitude, sighting.latitude)
                                var added = false
                                for ((existingLocation, sightingsList) in sightingsByLocation) {
                                    if (distanceBetween(sightingLatLng, existingLocation) <= 50) {
                                        sightingsList.add(sighting)
                                        added = true
                                        break
                                    }
                                }
                                if (!added) {
                                    sightingsByLocation[sightingLatLng] = mutableListOf(sighting)
                                }
                            }
                        }
                        updateMapMarkers(allHotspots, userSightings)
                    }
                } catch (e: Exception) {
                    Log.w("MapsActivity", "Error fetching user sightings", e)
                }
            }
        }
    }


    private fun prepareMarkerIcon(drawableResId: Int): BitmapDescriptor {
        val density = resources.displayMetrics.density
        val heightInPixels = (24 * density).toInt()
        val widthInPixels = (24 * density).toInt()
        val drawable = ContextCompat.getDrawable(this, drawableResId)!!
        val bitmap = drawableToBitmap(drawable)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, widthInPixels, heightInPixels, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }

}

