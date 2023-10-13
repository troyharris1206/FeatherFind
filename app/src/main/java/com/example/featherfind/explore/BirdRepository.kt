package com.example.featherfind.explore

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton repository for managing bird-related data.
 */
object BirdRepository {

    // Initialize OkHttpClient with interceptor to attach API token to requests
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original: Request = chain.request()
            val requestBuilder: Request.Builder = original.newBuilder()
                // Add API token to request header
                .header("X-eBirdApiToken", com.example.featherfind.BuildConfig.EBIRD_API_KEY)
            val request: Request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    // Initialize Retrofit instance with base URL and Gson converter
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.ebird.org/v2/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Create an API interface instance
    private val api: EBirdAPI = retrofit.create(EBirdAPI::class.java)

    /**
     * Fetches bird data based on latitude and longitude.
     *
     * @param lat The latitude coordinate.
     * @param lng The longitude coordinate.
     * @return A list of birds or null if the API call fails.
     */
    suspend fun getBirdsByLatLng(lat: Double?, lng: Double?): List<Bird>? {
        return try {
            val response: Response<List<Bird>> = api.getBirdsByLatLng(lat ?: 0.0, lng ?: 0.0)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("BirdRepository", "API call failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BirdRepository", "Exception occurred: ${e.message}")
            null
        }
    }

    /**
     * Fetches hotspot data based on latitude and longitude.
     *
     * @param lat The latitude coordinate.
     * @param lng The longitude coordinate.
     * @return A list of hotspots or null if the API call fails.
     */
    suspend fun getHotspotsByLatLng(lat: Double?, lng: Double?): List<Hotspot>? {
        return try {
            val response: Response<List<Hotspot>> = api.getHotspotsByLatLng(lat ?: 0.0, lng ?: 0.0)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("BirdRepository", "API call for hotspots failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BirdRepository", "Exception occurred while fetching hotspots: ${e.message}")
            null
        }
    }
}
