package com.cirrusnav.cirrusnav

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.json.JSONObject
import kotlin.math.abs

private const val TAG = "WeatherPrepper"

data class WeatherResult(
    val tempC: Double,
    val condition: String,
    val iconEmoji: String,
    val etaHours: Double,
    val precip_mm: Double,
)

class WeatherPrepper {
    private val client = HttpClient(CIO)

    /**
     * Fetches the forecast for a specific location and time offset (ETA).
     *
     * @param lat Latitude of the checkpoint
     * @param lon Longitude of the checkpoint
     * @param etaSeconds How many seconds from NOW the user will arrive there
     */
    suspend fun getWeatherAtETA(lat: Double, lon: Double, etaSeconds: Double): WeatherResult? {
        if (Keep.weatherApiKey.isBlank()) {
            Log.e(TAG, "getWeatherAtETA: WeatherAPI key is missing")
            return null
        }

        // We ask for 2 days of forecast to cover trips crossing midnight
        val url = "https://api.weatherapi.com/v1/forecast.json" +
                "?key=${Keep.weatherApiKey}" +
                "&q=$lat,$lon" +
                "&days=2" +
                "&aqi=no&alerts=no"

        return try {
            Log.d(TAG, "Fetching weather for $lat, $lon (ETA: ${etaSeconds}s)")
            val response: HttpResponse = client.get(url)
            val body = response.bodyAsText()

            if (response.status.value != 200) {
                Log.e(TAG, "WeatherAPI failed: $body")
                return null
            }

            val json = JSONObject(body)
            val forecastDays = json.getJSONObject("forecast").getJSONArray("forecastday")
            
            val targetEpoch = (System.currentTimeMillis() / 1000) + etaSeconds.toLong()

            var bestHour: JSONObject? = null
            var smallestDiff = Long.MAX_VALUE

            // Loop through all days and hours to find the hour closest to our ETA
            for (i in 0 until forecastDays.length()) {
                val hours = forecastDays.getJSONObject(i).getJSONArray("hour")
                for (j in 0 until hours.length()) {
                    val hourObj = hours.getJSONObject(j)
                    val hourEpoch = hourObj.getLong("time_epoch")
                    val diff = abs(hourEpoch - targetEpoch)
                    
                    if (diff < smallestDiff) {
                        smallestDiff = diff
                        bestHour = hourObj
                    }
                }
            }

            if (bestHour != null) {
                val tempC = bestHour.getDouble("temp_c")
                val conditionText = bestHour.getJSONObject("condition").getString("text")
                val precipmm: Double = bestHour.getDouble("precip_mm")
                val isDay = bestHour.optInt("is_day", 1) == 1
                
                val emoji = getEmojiForCondition(conditionText, isDay)
                
                return WeatherResult(
                    tempC = tempC,
                    condition = conditionText,
                    iconEmoji = emoji,
                    etaHours = etaSeconds / 3600.0,
                    precip_mm = precipmm
                )
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Weather parse error", e)
            null
        }
    }

    private fun getEmojiForCondition(condition: String, isDay: Boolean): String {
        val lower = condition.lowercase()
        return when {
            "rain" in lower || "drizzle" in lower -> "🌧️"
            "snow" in lower || "blizzard" in lower || "sleet" in lower -> "❄️"
            "thunder" in lower -> "⛈️"
            "cloud" in lower || "overcast" in lower -> "☁️"
            "mist" in lower || "fog" in lower -> "🌫️"
            "clear" in lower || "sunny" in lower -> if (isDay) "☀️" else "🌙"
            else -> if (isDay) "🌤️" else "🌙"
        }
    }
}
