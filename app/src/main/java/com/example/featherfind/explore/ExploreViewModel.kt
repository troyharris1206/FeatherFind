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

/**
 * ViewModel class for the Explore feature.
 *
 * This ViewModel manages the data and logic for the ExploreFragment.
 */
class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    val isLoading = MutableLiveData<Boolean>()
    val birdList = MutableLiveData<List<Bird>?>()
    private val allDataLoaded = MediatorLiveData<Boolean>()
    val filteredBirdList = MutableLiveData<List<Bird>?>()
    private var isDataAlreadyLoaded = false
    private var fetchBirdsJob: Job? = null
    private val viewModelJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val birdHistogramData = MutableLiveData<Map<String, List<Double>>>()

    /**
     * Filters the bird list based on a query string.
     *
     * @param query The string to filter the bird list by.
     */
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

    /**
     * Asynchronously fetches bird data.
     *
     * @return A list of Bird objects or null if the fetch fails.
     */
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

    /**
     * Retrieves the last known location.
     *
     * @return A Location object or null if the location is not available.
     */
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

    /**
     * Checks if location permissions are granted.
     *
     * @return True if permissions are granted, false otherwise.
     */
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

    /**
     * Initiates the fetching of bird data and histogram data.
     */
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
    /**
     * Loads histogram data for birds.
     *
     * @param filePath The file path of the histogram data.
     * @return A map from bird names to lists of histogram data.
     */
    private suspend fun internalLoadHistogramData(filePath: String): Map<String, List<Double>> {
        return withContext(Dispatchers.IO) {
            // Get the bird names from the API fetched list
            val apiBirdNames = birdList.value?.map { it.comName }?.toSet() ?: setOf()

            // Pass the API fetched bird names to the DataParser to find the histogram data
            DataParser.parseHistogramData(getApplication(), filePath, apiBirdNames)
        }
    }

    /**
     * Cancels all running coroutines when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        coroutineScope.cancel()  // This will cancel all the child jobs as well
    }
}
