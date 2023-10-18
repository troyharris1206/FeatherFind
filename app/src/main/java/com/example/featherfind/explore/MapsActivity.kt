package com.example.featherfind.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.featherfind.R
import com.example.featherfind.databinding.ActivityMapsBinding
import com.example.featherfind.explore.BirdRepository.getGoogleDirections
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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
import com.google.maps.android.PolyUtil
import org.json.JSONException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    /**
     * Called when the activity is created.
     * Initializes the map, ViewModel, and other UI components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

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

        requestUserLocation()

        val distanceSeekBar: SeekBar = findViewById(R.id.distanceSeekBar)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch maxDistance from Firestore
            val userDocument = db.collection("Users").document(currentUser.uid)
            userDocument.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val storedMaxDistance = document.getString("maxDistance")?.toFloatOrNull()
                        if (storedMaxDistance != null) {
                            maxDistance = storedMaxDistance
                            Log.d("Firestore", "Fetched maxDistance: $maxDistance")

                            // Set SeekBar max and progress here
                            distanceSeekBar.max = maxDistance.toInt()
                            distanceSeekBar.progress = maxDistance.toInt()
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
        distanceSeekBar.max = maxDistance.toInt()

        // Set SeekBar Listener
        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxDistance = progress.toFloat()
                filterHotspotsByDistance()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional
            }
        })
    }

    /**
     * Filters the list of hotspots based on their distance from the user's current location.
     * Only hotspots within 'maxDistance' meters are included.
     * After filtering, updates the map markers to reflect the filtered list.
     */
    private fun filterHotspotsByDistance() {
        // Filter all hotspots based on distance from the user's location
        val filteredHotspots = allHotspots.filter { hotspot ->
            val hotspotLocation = LatLng(hotspot.longitude, hotspot.latitude)
            val distance = distanceBetween(userLocation, hotspotLocation)
            distance <= maxDistance  // Include the hotspot if it is within maxDistance
        }
        // Update map markers based on filtered hotspots
        updateMapMarkers(filteredHotspots)
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
    private fun drawRoute(directions: String) {
        try {
            // Parse the JSON response
            val jsonResponse = JSONObject(directions)
            val routesArray = jsonResponse.getJSONArray("routes")

            // Clear previous direction steps to prepare for new directions
            directionSteps.clear()

            // Check if there are available routes
            if (routesArray.length() > 0) {
                val route = routesArray.getJSONObject(0)
                val legs = route.getJSONArray("legs")

                // Check if there are available legs
                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)

                    // Extract travel distance and time
                    travelDistance = leg.getJSONObject("distance").getString("text")
                    travelTime = leg.getJSONObject("duration").getString("text")

                    // Parse direction steps
                    val steps = leg.getJSONArray("steps")
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        var instruction = step.getString("html_instructions")
                        instruction = Html.fromHtml(instruction).toString()
                        directionSteps.add(instruction)
                    }

                    // Display the direction steps in a bottom sheet
                    if (selectedHotspotName != null) {
                        showDirectionSteps(selectedHotspotName!!)
                    } else {
                        // Handle case where selectedHotspotName is null
                        Log.e("MapsActivity", "No hotspot name available.")
                    }
                }

                // Clear previous polyline if any
                currentPolyline?.remove()

                // Draw the new polyline on the map
                val poly = route.getJSONObject("overview_polyline")
                val polyline = poly.getString("points")
                val decodedPath = PolyUtil.decode(polyline)
                currentPolyline = mMap.addPolyline(PolylineOptions().addAll(decodedPath))

                // Adjust the camera view to include all route points
                val builder = LatLngBounds.Builder()
                for (point in decodedPath) {
                    builder.include(point)
                }
                val bounds = builder.build()
                val padding = 200  // offset from edges of the map in pixels
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                mMap.moveCamera(cu)
            } else {
                Log.e("MapsActivity", "No routes available.")
            }
        } catch (e: JSONException) {
            Log.e("MapsActivity", "JSON parsing error: ${e.message}")
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

        // Fetch user preference for distance unit (either "miles" or "kilometers")
        firestore.collection("Users").document(currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val preferredUnit = document.getString("measurementSystem") ?: "Metric"

                // Extract numerical part from the string (assuming the format is always like "4.2 km")
                val numericalPart = travelDistance.split(" ")[0]

                // Convert distance to miles if necessary
                val displayedTravelDistance = if (preferredUnit == "Imperial") {
                    val miles = numericalPart.toDouble() * 0.621371  // Conversion factor for km to miles
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
    private suspend fun getDirections(context: Context, origin: LatLng, destination: LatLng) {
        val response = getGoogleDirections(context, origin, destination)
        if (response != null) {
            drawRoute(response)
        } else {
            Log.e("MapsActivity", "Google Directions API returned null.")
        }
    }

    /**
     * Called when the Google Map is ready to be used.
     * Configures initial settings for the map and sets listeners.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable UI controls like zoom and current location button
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Check for location permission
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

        // Add markers to the map as soon as it's ready
        updateMapMarkers(allHotspots)

        // Set marker click listener
        mMap.setOnMarkerClickListener { marker ->
            onMarkerClick(marker)
            selectedHotspotName = marker.tag as? String ?: marker.title
            false // or true, depending on whether you want to consume the event
        }
        // Add map click listener
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
    private fun updateMapMarkers(hotspots: List<Hotspot>) {
        // Clear existing markers
        mMap.clear()

        // Loop through all hotspots to add them as markers
        for (hotspot in hotspots) {
            val hotspotLocation = LatLng(hotspot.longitude, hotspot.latitude)

            // Fetch drawable resource for marker icon
            val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24)!!

            // Convert drawable to bitmap
            val bitmap = drawableToBitmap(drawable)

            // Create a BitmapDescriptor from the bitmap
            val markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap)

            // Add the marker with custom icon to the map
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(hotspotLocation)
                    .title(hotspot.name)
                    .icon(markerIcon)
            )

            // Attach the hotspot name as additional data to the marker
            marker?.tag = hotspot.name
        }
        // Move the camera to the user's location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
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
        // Remove the existing polyline, if any
        currentPolyline?.remove()

        // Get the destination LatLng from the marker
        val destination = marker.position

        // Launch a coroutine to fetch directions to the clicked marker
        lifecycleScope.launch {
            getDirections(this@MapsActivity, userLocation, destination)
        }

        return true  // Consumed the click event
    }
}