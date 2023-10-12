package com.example.featherfind.explore

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.featherfind.explore.BirdRepository
import com.example.featherfind.explore.Hotspot

class HotspotViewModel : ViewModel() {
    private val isLoading = MutableLiveData<Boolean>()
    val hotspotList = MutableLiveData<List<Hotspot>?>()

    suspend fun fetchHotspots(lat: Double?, lng: Double?) {
        isLoading.postValue(true)
        try {
            val hotspots = BirdRepository.getHotspotsByLatLng(lat, lng)
            hotspotList.postValue(hotspots)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading.postValue(false)
        }
    }
}
