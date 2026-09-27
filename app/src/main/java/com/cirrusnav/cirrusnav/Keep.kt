package com.cirrusnav.cirrusnav

import de.afarber.openmapview.GeoJsonResult
import de.afarber.openmapview.LatLng

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
    // TODO: Paste your WeatherAPI key here
    val weatherApiKey = "ad6132620fee479ab0e214447262609"

    // TODO: Paste your ORS API key
    val openRouteServiceKey: String = ""

    fun isOpenRouteServiceKeyValid(): Boolean {
        return openRouteServiceKey.isNotEmpty() && !openRouteServiceKey.contains("&")
    }

    var path: GeoJsonResult? = null

    // Mutable route endpoints — set by the UI when user enters addresses
    var startPos: LatLng? = null
    var destPos: LatLng? = null
}