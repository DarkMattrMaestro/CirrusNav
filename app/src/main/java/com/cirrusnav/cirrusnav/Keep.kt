package com.cirrusnav.cirrusnav

/**
 * Global application state for API keys and route parameters.
 *
 * API keys needed:
 *   - OpenRouteService (ORS): Free at https://account.heigit.org
 *     Used for: Directions (routing) and Geocoding (address → coordinates)
 *   - WeatherAPI: Free at https://www.weatherapi.com
 *     Used for: Fetching weather along the route
 *
 * No API key needed for:
 *   - OpenMapView: Uses OpenStreetMap tiles (free, no key)
 */
object Keep {
    var routeDurationSeconds = 0.0

    // TODO: Paste your WeatherAPI key here
    val weatherApiKey = "ad6132620fee479ab0e214447262609"

    // TODO: Paste your ORS API key
    val openRouteServiceKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjMyYTVjZWRjY2E5YjQ1NmQ4ZjA4MGJiYTJiZDhiYjU2IiwiaCI6Im11cm11cjY0In0="

    fun isOpenRouteServiceKeyValid(): Boolean {
        return openRouteServiceKey.isNotEmpty() && !openRouteServiceKey.contains("&")
    }

    // Mutable route endpoints — set by the UI when user enters addresses
    var startPos: GeoCoord? = null
    var destPos: GeoCoord? = null

    var k_points = mutableListOf<de.afarber.openmapview.LatLng>()
}