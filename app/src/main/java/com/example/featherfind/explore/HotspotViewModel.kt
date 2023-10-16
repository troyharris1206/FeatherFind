package com.example.featherfind.explore

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.featherfind.explore.BirdRepository
import com.example.featherfind.explore.Hotspot

class HotspotViewModel : ViewModel() {
    private val isLoading = MutableLiveData<Boolean>()
    val hotspotList = MutableLiveData<List<Hotspot>?>()

    // Add a region code variable
    private var regionCode: String = ""

    suspend fun setRegionCode(code: String) {
        this.regionCode = code
        // Re-fetch or re-filter hotspots here
        fetchHotspots()
    }

    suspend fun fetchHotspots() {
        isLoading.postValue(true)
        try {
            if (regionCode.isNotEmpty()) {
                // Use the regionCode to fetch hotspots for the specified region
                val hotspots = BirdRepository.getHotspotsByRegion(regionCode)
                hotspotList.postValue(hotspots)
            } else {
                // Handle the case where regionCode is empty
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading.postValue(false)
        }
    }
}

