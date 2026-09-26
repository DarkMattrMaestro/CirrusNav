package com.cirrusnav.cirrusnav

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

/**
 * Allowed transportation modes for pathfinding.
 *
 * See https://openrouteservice.org/dev/#/api-docs/openrouteservice/v2/directions/{profile}/get for more info
 */
enum class TransportationMode(val representation: String) {
    drivingCar("driving-car")
}

class PathPrepper {
    suspend fun getRoute(): GeoJsonResult? {
        val client = HttpClient(CIO)

        // Guard against missing API parameters
        if (!Keep.isOpenRouteServiceKeyValid() || !Keep.startPos.isValid() || !Keep.destPos.isValid()) {
            return null
        }

        val transportationMode: TransportationMode = TransportationMode.drivingCar

        val response: HttpResponse = client.get(
            "https://api.heigit.org/openrouteservice/v2/directions/" + transportationMode.representation
                    + "?api_key=" + Keep.openRouteServiceKey
                    + "&start=" + Keep.startPos
                    + "&end=" + Keep.destPos
        )

        return GeoJsonParser.parse(response.toString()) // TODO: Find better way to convert than to string and back
    }
}