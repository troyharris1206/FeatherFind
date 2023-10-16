package com.example.featherfind.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.featherfind.R
import com.example.featherfind.databinding.ActivityMapsBinding
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002
    private lateinit var viewModel: HotspotViewModel
    private var maxDistance: Float = Float.MAX_VALUE
    private var userLocation: LatLng = LatLng(0.0, 0.0)

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
            addHotspotsToMap(hotspots)
        }
        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxDistance = progress.toFloat()
                filterHotspotsByDistance()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Request the user's location
        requestUserLocation()
    }
private var allHotspots: List<Hotspot> = listOf()

    private fun filterHotspotsByDistance() {
        val filteredHotspots = allHotspots.filter { hotspot ->
            val hotspotLocation = LatLng(hotspot.latitude, hotspot.longitude)
            // Assuming you have a method or variable `userLocation` that holds the LatLng of the user
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
private fun updateMapMarkers(hotspots: List<Hotspot>) {
    mMap.clear()
    for (hotspot in hotspots) {
        val hotspotLocation = LatLng(hotspot.latitude, hotspot.longitude)
        mMap.addMarker(
            MarkerOptions()
                .position(hotspotLocation)
                .title(hotspot.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }
}
    private fun addHotspotsToMap(hotspots: List<Hotspot>?) {
        if (!hotspots.isNullOrEmpty()) {
            allHotspots = hotspots
            updateMapMarkers(allHotspots)
        } else {
            Log.d("MapsActivity", "No hotspots to add to the map.")
        }
    }

    private fun drawRouteTo(destination: LatLng) {
        // Use Google Directions API to get the route and draw it on the map
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
        mMap.setOnMarkerClickListener { marker ->
            val destination = marker.position

            // call a function to draw the best route
            drawRouteTo(destination)
            false
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
            val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
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
}