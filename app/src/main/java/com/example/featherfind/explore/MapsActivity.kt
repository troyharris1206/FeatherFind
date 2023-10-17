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
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import org.json.JSONException
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002
    private lateinit var viewModel: HotspotViewModel
    private var maxDistance: Float = 50000f  // 50km in meters
    private var userLocation: LatLng = LatLng(0.0, 0.0)
    private var allHotspots: List<Hotspot> = listOf()
    private var travelDistance: String = ""
    private var travelTime: String = ""
    private var directionSteps: ArrayList<String> = ArrayList()  // To hold direction steps
    private var currentPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val distanceSeekBar: SeekBar = findViewById(R.id.distanceSeekBar)
        viewModel = ViewModelProvider(this).get(HotspotViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        viewModel.hotspotList.observe(this) { hotspots ->
            allHotspots = hotspots ?: listOf()
            filterHotspotsByDistance()

        }
        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxDistance = progress.toFloat()
                filterHotspotsByDistance()
                distanceSeekBar.max = 50000
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Request the user's location
        requestUserLocation()
    }

    private fun filterHotspotsByDistance() {
        val filteredHotspots = allHotspots.filter { hotspot ->
            val hotspotLocation = LatLng(hotspot.longitude, hotspot.latitude)
            val distance = distanceBetween(userLocation, hotspotLocation)
            distance <= maxDistance
        }
        updateMapMarkers(filteredHotspots)
    }

    private fun distanceBetween(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    private fun drawRoute(directions: String) {
        try {
            val jsonResponse = JSONObject(directions)
            val routesArray = jsonResponse.getJSONArray("routes")

            // Clear previous direction steps
            directionSteps.clear()

            if (routesArray.length() > 0) {
                val route = routesArray.getJSONObject(0)
                val legs = route.getJSONArray("legs")

                if (legs.length() > 0) {
                    val leg = legs.getJSONObject(0)
                    travelDistance = leg.getJSONObject("distance").getString("text")
                    travelTime = leg.getJSONObject("duration").getString("text")

                    val steps = leg.getJSONArray("steps")
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        var instruction = step.getString("html_instructions")
                        instruction = Html.fromHtml(instruction).toString()
                        directionSteps.add(instruction)
                    }
                    // Show direction steps to user
                    showDirectionSteps()
                }
                currentPolyline?.remove()
                val poly = route.getJSONObject("overview_polyline")
                val polyline = poly.getString("points")
                val decodedPath = PolyUtil.decode(polyline)
                mMap.addPolyline(PolylineOptions().addAll(decodedPath))
                currentPolyline = mMap.addPolyline(PolylineOptions().addAll(decodedPath))

            } else {
                Log.e("MapsActivity", "No routes available.")
            }
        } catch (e: JSONException) {
            Log.e("MapsActivity", "JSON parsing error: ${e.message}")
        }
    }

    private fun showDirectionSteps() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Direction Steps")

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, directionSteps)
        builder.setAdapter(arrayAdapter, null)

        val footerView = layoutInflater.inflate(R.layout.dialog_footer, null)
        footerView.findViewById<TextView>(R.id.travel_distance).text = "Distance: $travelDistance"
        footerView.findViewById<TextView>(R.id.travel_time).text = "Time: $travelTime"

        builder.setView(footerView)

        builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }

        builder.show()
    }


    suspend fun getDirections(context: Context, origin: LatLng, destination: LatLng) {
        val response = getGoogleDirections(context, origin, destination)
        if (response != null) {
            drawRoute(response)
        } else {
            Log.e("MapsActivity", "Google Directions API returned null.")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapsActivity", "onMapReady called.")
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling ActivityCompat#requestPermissions here if needed
                return
            }
            mMap.isMyLocationEnabled = true
        }
        updateMapMarkers(allHotspots)  // Ensure markers are added as soon as map is ready
        mMap.setOnMarkerClickListener { marker -> onMarkerClick(marker) }
    }

    private fun updateMapMarkers(hotspots: List<Hotspot>) {
        mMap.clear()
        for (hotspot in hotspots) {
            val hotspotLocation = LatLng(hotspot.longitude, hotspot.latitude)

            // Get the drawable resource
            val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24)!!

            // Convert the drawable to a bitmap
            val bitmap = drawableToBitmap(drawable)

            // Create a BitmapDescriptor from the bitmap
            val markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap)

            // Add the marker to the map with the custom icon
            mMap.addMarker(
                MarkerOptions()
                    .position(hotspotLocation)
                    .title(hotspot.name)
                    .icon(markerIcon)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
        }
    }

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

    private fun requestUserLocation() {
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling ActivityCompat#requestPermissions
                return
            }

            // Launch a coroutine to handle location retrieval
            lifecycleScope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        // Get the user's location
                        userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 10f))

                        // Determine the region code based on the user's location
                        val regionCode = determineRegionCode(this@MapsActivity, location)

                        // Set the region code in the ViewModel
                        viewModel.setRegionCode(regionCode)

                        // Call fetchHotspots from within a coroutine
                        viewModel.fetchHotspots()
                    } else {
                        // Handle the case where location is null
                    }
                } catch (exception: Exception) {
                    Log.e("MapsActivity", "Failed to get user location: ${exception.message}")
                }
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun determineRegionCode(context: Context, location: Location): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: List<Address>? =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                // Get the first address, which is usually the most accurate one
                val address = addresses[0]

                // Extract the country code
                val countryCode = address.countryCode ?: "unknown_country"

                // Use the country code as the region code
                return countryCode
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Default to an unknown region code if reverse geocoding fails
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
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun onMarkerClick(marker: Marker): Boolean {
        currentPolyline?.remove()
        val destination = marker.position
        lifecycleScope.launch {
            getDirections(this@MapsActivity, userLocation, destination)
        }
        return true
    }
}