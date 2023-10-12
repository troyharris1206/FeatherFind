package com.example.featherfind.explore


import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object BirdRepository {
    // Initialize OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original: Request = chain.request()
            val requestBuilder: Request.Builder = original.newBuilder()
                // Add API key to the request header. Replace 'Authorization' with the actual header field name
                // as per your API's documentation.
                .header("X-eBirdApiToken", com.example.featherfind.BuildConfig.EBIRD_API_KEY)
            val request: Request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    // Initialize Retrofit and the EBirdAPI service
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.ebird.org/v2/")  // Replace with your actual base URL
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: EBirdAPI = retrofit.create(EBirdAPI::class.java)

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


