package com.example.featherfind.explore

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

import retrofit2.Response
import retrofit2.http.Path

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
    suspend fun getBirdsByLatLng(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<List<Bird>>


    /**
     * Fetches a list of bird-watching hotspots by a given region code.
     *
     * This function is a coroutine and is designed to be called in a suspended context.
     * It interacts with the "/ref/hotspot/{regionCode}" endpoint of the eBird API to fetch data.
     *
     * @param regionCode The code representing a specific geographical region.
     *                   This parameter will be included in the API's endpoint URL.
     *
     * @return A Response object containing either:
     *          - A list of Hotspot objects encapsulated in a ResponseBody, or
     *          - An error message in the ResponseBody.
     *
     * @throws IOException if there is a failure during the execution of the HTTP request.
     */
    @GET("ref/hotspot/{regionCode}")
    suspend fun getHotspotsByRegion(@Path("regionCode") regionCode: String): Response<ResponseBody>
}



