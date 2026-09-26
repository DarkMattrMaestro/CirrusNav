package com.cirrusnav.cirrusnav

import android.util.Log
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.GeoJsonParser
import de.afarber.openmapview.GeoJsonResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.json.JSONObject

/**
 * Allowed transportation modes for pathfinding.
 *
 * See https://openrouteservice.org/dev/#/api-docs/openrouteservice/v2/directions/{profile}/get for more info
 */
enum class TransportationMode(val representation: String) {
    drivingCar("driving-car")
}

class PathPrepper {

    fun setRoute(){

        var StartCoord = GeoCoord()
        StartCoord.latitude = 43.8127
        StartCoord.longitude = -79.2941


        var FinalCoord = GeoCoord()
        FinalCoord.latitude =  45.4215
        FinalCoord.longitude = -75.6972

        Keep.startPos = StartCoord
        Keep.destPos = FinalCoord

    }


    suspend fun getRoute(): GeoJsonResult? {
        val client = HttpClient(CIO)

        // Guard against missing API parameters
        if (!Keep.isOpenRouteServiceKeyValid() || !Keep.startPos.isValid() || !Keep.destPos.isValid()) {
            System.out.println("On is Open: ${Keep.isOpenRouteServiceKeyValid()}, On startpos isValid: ${Keep.startPos.isValid()}, on destpos: ${Keep.destPos.isValid()}")
            return null
        }

        val transportationMode: TransportationMode = TransportationMode.drivingCar

        val response: HttpResponse = client.get(
            "https://api.heigit.org/openrouteservice/v2/directions/" + transportationMode.representation
                    + "?api_key=" + Keep.openRouteServiceKey
                    + "&start=${Keep.startPos.toURLString()}"
                    + "&end=${Keep.destPos.toURLString()}"
        )

        System.out.println(response)

        val body = response.bodyAsText()
        try {
            val json = JSONObject(body)
            val features = json.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val props = feature.optJSONObject("properties")
                    ?: JSONObject().also { feature.put("properties", it) }

                props.put("stroke", "#1E88E5")
                props.put("stroke-width", 5)
                props.put("stroke-opacity", 1.0)
                props.put("fill", "#1E88E5")
            }

            val geo = GeoJsonParser.parse(json.toString())

            // use geo
            return geo
        } catch (e: Exception) {
            Log.e("Route", "Bad GeoJSON: ${body.take(300)}", e)
            return null
        }

        //return GeoJsonParser.parse(response.toString()) // TODO: Find better way to convert than to string and back
    }
}