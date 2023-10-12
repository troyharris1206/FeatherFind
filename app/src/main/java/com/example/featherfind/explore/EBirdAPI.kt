package com.example.featherfind.explore

import com.example.featherfind.explore.Bird
import com.example.featherfind.explore.Hotspot
import retrofit2.http.GET
import retrofit2.http.Query

import retrofit2.Response

interface EBirdAPI {
    @GET("data/obs/geo/recent")
    suspend fun getBirdsByLatLng(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<Bird>>

    @GET("ref/hotspot/geo")
    suspend fun getHotspotsByLatLng(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<Hotspot>>
}



