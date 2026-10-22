package com.vihaan.myjourney.destination

import com.vihaan.myjourney.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Encapsulates communication with the Geoapify Geocoding & Autocomplete API.
 *
 * In Java terms: A service/client repository class separating network concerns from the UI layer.
 */
class GeoapifyClient(
    private val apiKey: String = BuildConfig.GEOAPIFY_API_KEY,
    private val httpClient: OkHttpClient = defaultHttpClient
) {

    companion object {
        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    /**
     * Searches for destinations matching the given query text.
     * Runs asynchronously on Dispatchers.IO.
     *
     * @param query Search text entered by the user.
     * @param proximityLat Optional latitude of the user's current location for proximity biasing.
     * @param proximityLon Optional longitude of the user's current location for proximity biasing.
     * @return Result containing a list of [Destination] candidates or an exception.
     */
    suspend fun searchDestinations(
        query: String,
        proximityLat: Double? = null,
        proximityLon: Double? = null
    ): Result<List<Destination>> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Geoapify API key not configured.\nPlease add GEOAPIFY_API_KEY=YOUR_KEY to local.properties."
                )
            )
        }

        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("api.geoapify.com")
            .addPathSegment("v1")
            .addPathSegment("geocode")
            .addPathSegment("autocomplete")
            .addQueryParameter("text", trimmedQuery)
            .addQueryParameter("limit", "5")
            .addQueryParameter("filter", "countrycode:in")
            .addQueryParameter("apiKey", apiKey)

        // If current location is available, bias suggestions near the user
        if (proximityLat != null && proximityLon != null) {
            urlBuilder.addQueryParameter("bias", "proximity:$proximityLon,$proximityLat")
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = when (code) {
                        401, 403 -> "Invalid or unauthorized Geoapify API key ($code)."
                        429 -> "Rate limit reached for Geoapify API ($code)."
                        else -> "Search request failed with HTTP $code."
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val responseBody = response.body?.string().orEmpty()
                val destinations = parseGeoapifyFeatures(responseBody)
                Result.success(destinations)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeoapifyFeatures(json: String): List<Destination> {
        val results = mutableListOf<Destination>()
        val root = JSONObject(json)
        val features = root.optJSONArray("features") ?: return emptyList()

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val properties = feature.optJSONObject("properties") ?: continue

            val formatted = properties.optString("formatted").takeIf { it.isNotBlank() }
                ?: properties.optString("name", "Unknown location")
            val name = properties.optString("name").takeIf { it.isNotBlank() } ?: formatted
            val placeId = properties.optString("place_id", i.toString())
            val lat = properties.optDouble("lat", Double.NaN)
            val lon = properties.optDouble("lon", Double.NaN)

            if (!lat.isNaN() && !lon.isNaN()) {
                results.add(
                    Destination(
                        placeId = placeId,
                        name = name,
                        formattedAddress = formatted,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
        }
        return results
    }
}
