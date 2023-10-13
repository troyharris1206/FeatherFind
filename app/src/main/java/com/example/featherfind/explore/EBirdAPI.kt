package com.example.featherfind.explore

import retrofit2.http.GET
import retrofit2.http.Query

import retrofit2.Response

/**
 * Defines the API endpoints for interacting with the eBird service.
 */
interface EBirdAPI {

    /**
     * Fetches a list of recent bird observations for a given geographic location.
     *
     * This API endpoint retrieves bird data based on latitude and longitude.
     *
     * @param lat The latitude coordinate of the location.
     * @param lng The longitude coordinate of the location.
     * @return A Response object containing a list of Bird objects or an error.
     */
    @GET("data/obs/geo/recent")
    suspend fun getBirdsByLatLng(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<Bird>>

    /**
     * Fetches a list of hotspots for a given geographic location.
     *
     * This API endpoint retrieves hotspot data based on latitude and longitude.
     *
     * @param lat The latitude coordinate of the location.
     * @param lng The longitude coordinate of the location.
     * @return A Response object containing a list of Hotspot objects or an error.
     */
    @GET("ref/hotspot/geo")
    suspend fun getHotspotsByLatLng(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<List<Hotspot>>
}



