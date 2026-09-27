package com.cirrusnav.cirrusnav

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.GeoJsonParser
import de.afarber.openmapview.GeoJsonResult
import de.afarber.openmapview.LatLng
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.net.URLEncoder

private const val TAG = "PathPrepper"

/**
 * Allowed transportation modes for pathfinding.
 */
enum class TransportationMode(val representation: String) {
    DRIVING_CAR("driving-car")
}

data class RouteResult(
    val polylines: List<de.afarber.openmapview.Polyline>,
    val durationSeconds: Double,
    val distanceMeters: Double
)



private class FakeUser (Coords: GeoCoord){
    var location = GeoCoord(0.0,0.0)
        set(value) {
            field = value
        }

    init{
        location = Coords
    }




    @Composable
    fun DrawUser(){
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.Green)

        )
    }



}
class PathPrepper {
    private val client = HttpClient(CIO)

    /**
     * Fetches a route from ORS Directions API between Keep.startPos and Keep.destPos.
     */
    suspend fun getRoute(): RouteResult? {
        val start = Keep.startPos ?: run {
            Log.e(TAG, "getRoute: startPos is null")
            return null
        }
        val dest = Keep.destPos ?: run {
            Log.e(TAG, "getRoute: destPos is null")
            return null
        }

        if (!Keep.isOpenRouteServiceKeyValid()) {
            Log.e(TAG, "getRoute: API key is invalid or empty")
            return null
        }

        val transportationMode = TransportationMode.DRIVING_CAR

        val url = "https://api.heigit.org/openrouteservice/v2/directions/${transportationMode.representation}" +
                "?start=${start.longitude},${start.latitude}" +
                "&end=${dest.longitude},${dest.latitude}"

        Log.d(TAG, "getRoute URL: $url")

        val response: HttpResponse = client.get(url) {
            headers {
                append("Authorization", Keep.openRouteServiceKey)
            }
        }
        val body = response.bodyAsText()

        Log.d(TAG, "getRoute status: ${response.status}")
        Log.d(TAG, "getRoute body (first 500 chars): ${body.take(500)}")

        if (response.status.value != 200) {
            Log.e(TAG, "getRoute failed: $body")
            throw Exception("ORS returned ${response.status}: ${body.take(200)}")
        }

        return try {
            val json = org.json.JSONObject(body)
            val features = json.getJSONArray("features")
            val polylines = mutableListOf<de.afarber.openmapview.Polyline>()
            
            var totalDuration = 0.0
            var totalDistance = 0.0
            
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.optJSONObject("properties")
                val summary = properties?.optJSONObject("summary")
                if (summary != null) {
                    totalDuration += summary.optDouble("duration", 0.0)
                    totalDistance += summary.optDouble("distance", 0.0)
                }

                val geometry = feature.optJSONObject("geometry") ?: continue
                if (geometry.optString("type") == "LineString") {
                    val coordinates = geometry.getJSONArray("coordinates")
                    val points = mutableListOf<de.afarber.openmapview.LatLng>()
                    for (j in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(j)
                        // ORS returns [longitude, latitude]
                        points.add(de.afarber.openmapview.LatLng(coord.getDouble(1), coord.getDouble(0)))

                    }
                    if (points.size >= 2) {
                        polylines.add(de.afarber.openmapview.Polyline(points = points))
                    }
                    Keep.k_points = points
                }
            }

            Keep.routeDurationSeconds = totalDuration
            val result = RouteResult(polylines, totalDuration, totalDistance)
            Log.d(TAG, "Parsed: ${result.polylines.size} polylines, duration=${totalDuration}s")
            result
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}", e)
            throw e
        }
    }


    fun GetClosestPoints(coords: GeoCoord): LatLng? {
        val points = Keep.k_points
        if (points.isEmpty()) {
            Log.w(TAG, "GetClosestPoints: no route points yet")
            return null
        }

        // Longitude degrees shrink toward the poles; scale by cos(lat) so the comparison is fair
        val cosLat = Math.cos(Math.toRadians(coords.latitude))

        val closest = points.minBy { p ->
            val dLat = p.latitude - coords.latitude
            val dLon = (p.longitude - coords.longitude) * cosLat
            dLat * dLat + dLon * dLon   // squared distance is fine for comparing
        }

        Log.d(TAG, "Closest point to $coords is $closest")
        return closest
    }

    /**
     * Geocodes an address using Nominatim
     *
     * Rate limited to 1 request/sec.
     * https://nominatim.openstreetmap.org/search
     */
    suspend fun geocode(address: String): GeoCoord? {
        if (address.isBlank()) {
            Log.e(TAG, "geocode: address is blank")
            return null
        }

        val encodedAddress = URLEncoder.encode(address, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search" +
                "?q=$encodedAddress" +
                "&format=json" +
                "&limit=1"

        Log.d(TAG, "geocode URL: $url")

        val response: HttpResponse = client.get(url) {
            headers {
                // Nominatim requires a User-Agent to identify the app
                append("User-Agent", "CirrusNav/1.0")
            }
        }
        val body = response.bodyAsText()

        Log.d(TAG, "geocode status: ${response.status}")
        Log.d(TAG, "geocode body: ${body.take(500)}")

        if (response.status.value != 200) {
            Log.e(TAG, "geocode failed: $body")
            throw Exception("Nominatim returned ${response.status}")
        }

        return try {
            // Nominatim returns a JSON array, e.g.:
            // [{"lat":"45.4208777","lon":"-75.6901106","display_name":"Ottawa, ..."}]

            val results = org.json.JSONArray(body)
            if (results.length() > 0) {
                val first = results.getJSONObject(0)
                val result = GeoCoord(
                    longitude = first.getDouble("lon"),
                    latitude = first.getDouble("lat")
                )
                Log.d(TAG, "geocode '$address' -> $result")
                result
            } else {
                Log.w(TAG, "geocode '$address': no results")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "geocode parse error: ${e.message}", e)
            throw e
        }
    }
}