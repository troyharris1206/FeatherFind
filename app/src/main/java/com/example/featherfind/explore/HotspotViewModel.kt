package com.example.featherfind.explore

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel class responsible for managing hotspot data.
 */
class HotspotViewModel : ViewModel() {
    // LiveData to indicate if data fetching is in progress.
    private val isLoading = MutableLiveData<Boolean>()

    // LiveData to hold the list of fetched hotspots.
    val hotspotList = MutableLiveData<List<Hotspot>?>()

    // Variable to store the region code for filtering hotspots.
    private var regionCode: String = ""

    /**
     * Sets the region code and initiates the fetching of hotspots.
     *
     * @param code The region code to be set.
     */
    suspend fun setRegionCode(code: String) {
        this.regionCode = code
        // Re-fetch or re-filter the list of hotspots based on the new region code.
        fetchHotspots()
    }

    /**
     * Fetches hotspots based on the current region code.
     */
    suspend fun fetchHotspots() {
        // Indicate that fetching has started.
        isLoading.postValue(true)
        try {
            // Check if the region code is set.
            if (regionCode.isNotEmpty()) {
                // Fetch hotspots for the specified region using the BirdRepository.
                val hotspots = BirdRepository.getHotspotsByRegion(regionCode)
                // Update the LiveData with the fetched hotspots.
                hotspotList.postValue(hotspots)
            } else {
                // Handle the case where the region code is empty.
            }
        } catch (e: Exception) {
            // Handle any exceptions that may occur during fetching.
        } finally {
            // Indicate that fetching has completed.
            isLoading.postValue(false)
        }
    }
}
