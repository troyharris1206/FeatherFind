package com.example.featherfind.explore

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.lang.Exception
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    val isLoading = MutableLiveData<Boolean>()
    val birdList = MutableLiveData<List<Bird>?>()
    private val allDataLoaded = MediatorLiveData<Boolean>()
    val filteredBirdList = MutableLiveData<List<Bird>?>()
    private var isDataAlreadyLoaded = false
    private var fetchBirdsJob: Job? = null
    private val viewModelJob = SupervisorJob() // Use SupervisorJob to handle child coroutines
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val birdHistogramData = MutableLiveData<Map<String, List<Double>>>()

    // Call this function whenever you want to filter the birds
    fun filterBirds(query: String) {
        val originalList = birdList.value ?: emptyList()
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { bird ->
                bird.comName.contains(query, ignoreCase = true)
            }
        }
        filteredBirdList.value = filteredList
    }

    private suspend fun fetchBirds(): List<Bird>? {
        return withContext(Dispatchers.IO) {
            try {
                if (hasLocationPermission()) {
                    val locationResult = getLastLocation()
                    val latitude = locationResult?.latitude
                    val longitude = locationResult?.longitude

                    val birds = BirdRepository.getBirdsByLatLng(latitude, longitude)
                    birds
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Exception occurred: ${e.message}")
                null
            }
        }
    }

    private suspend fun getLastLocation(): Location? {
        return suspendCoroutine { continuation ->
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
                    // Handle SecurityException here
                    Log.e("ExploreViewModel", "SecurityException: ${securityException.message}")
                    continuation.resume(null)
                }
            } else {
                // Location permissions are not granted, request permissions
                ActivityCompat.requestPermissions(
                    getApplication(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                // Resume with null since permissions are not granted yet
                continuation.resume(null)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    fun initiateFetchBirdsAndHistograms() {
        if (isDataAlreadyLoaded) return
        if (fetchBirdsJob?.isActive == true) {
            return  // exit if a similar job is already running
        }

        fetchBirdsJob = coroutineScope.launch {
            isLoading.postValue(true)
            val fetchedBirds = fetchBirds()
            birdList.postValue(fetchedBirds)

            // Load histogram data
            val histogramData = internalLoadHistogramData("data.txt")
            birdHistogramData.postValue(histogramData)

            allDataLoaded.postValue(true)
            isDataAlreadyLoaded = true

            birdList.value = fetchedBirds?.map { bird ->
                bird.copy(histogramData = histogramData[bird.comName])
            }
            isLoading.postValue(false)
        }
    }
    private suspend fun internalLoadHistogramData(filePath: String): Map<String, List<Double>> {
        return withContext(Dispatchers.IO) {
            // Get the bird names from the API fetched list
            val apiBirdNames = birdList.value?.map { it.comName }?.toSet() ?: setOf()

            // Pass the API fetched bird names to the DataParser to find the histogram data
            DataParser.parseHistogramData(getApplication(), filePath, apiBirdNames)
        }
    }


    override fun onCleared() {
        super.onCleared()
        coroutineScope.cancel()  // This will cancel all the child jobs as well
    }
}
