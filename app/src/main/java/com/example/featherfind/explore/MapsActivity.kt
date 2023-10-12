package com.example.featherfind.explore

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002
    private lateinit var viewModel: HotspotViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProvider(this).get(HotspotViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Observe hotspot list
        viewModel.hotspotList.observe(this) { hotspots ->
            if (hotspots != null) {
                updateMapWithHotspots(hotspots)
            }
        }

        lifecycleScope.launch {
            val location: Location? = getLastLocation()
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                viewModel.fetchHotspots(latitude, longitude)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            if (hasLocationPermission()) {
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            continuation.resume(location)
                        }
                        .addOnFailureListener { exception: Exception ->
                            continuation.resumeWithException(exception)
                        }
                } catch (securityException: SecurityException) {
                    continuation.resume(null)
                }
            } else {
                requestLocationPermissions()
                continuation.resume(null)
            }
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

    private fun updateMapWithHotspots(hotspots: List<Hotspot>) {
        mMap.clear()  // Clear previous markers

        for (hotspot in hotspots) {
            val latLng = LatLng(hotspot.latitude, hotspot.longitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(hotspot.name)
            )
        }
    }
}
