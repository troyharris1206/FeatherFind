package com.example.featherfind.explore

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.featherfind.explore.DataParser.parseApiResponse
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
     * Fetches hotspot data based on region code.
     *
     * @param regionCode The code representing the region.
     * @return A list of hotspots or null if the API call fails.
     */
    suspend fun getHotspotsByRegion(regionCode: String?): List<Hotspot>? {
        return try {

            val response: Response<ResponseBody> = api.getHotspotsByRegion(regionCode ?: "")

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()

                if (responseBody != null) {
                    // Use DataParser's parseApiResponse function to parse the hotspot data.
                    parseApiResponse(responseBody)

                } else {
                    Log.e("BirdRepository", "API call successful but response body is null")
                    null
                }
            } else {
                Log.e("BirdRepository", "API call failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("BirdRepository", "Exception occurred while fetching hotspots: ${e.message}")
            null
        }
    }
    suspend fun getGoogleDirections(context: Context, origin: LatLng, destination: LatLng): String? = suspendCancellableCoroutine { cont ->
        val app = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val bundle = app.metaData
        val apiKey = bundle.getString("com.google.android.geo.API_KEY")

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("maps.googleapis.com")
            .addPathSegments("maps/api/directions/json")
            .addQueryParameter("origin", "${origin.latitude},${origin.longitude}")
            .addQueryParameter("destination", "${destination.latitude},${destination.longitude}")
            .addQueryParameter("key", apiKey)
            .build()
        val request = Request.Builder().url(url).build()

        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    cont.resume(response.body()?.string())
                } else {
                    cont.resumeWithException(IOException("Google Directions API call failed: ${response.body()?.string()}"))
                }
            }
        })
        cont.invokeOnCancellation {
            call.cancel()
        }
    }
}
