package com.cirrusnav.cirrusnav

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import de.afarber.openmapview.GeoJsonResult
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Polyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MapViewScreen()
                }
            }
        }
    }
}

@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }

    // Center on Ottawa
    val ottawaCenter = LatLng(45.4215, -75.6972)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(ottawaCenter)
                        setZoom(10.0f)
                        setOnAttributionClickListener {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.openstreetmap.org/copyright".toUri(),
                            )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
            ) {
                RouteControls(
                    mapView = mapView,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(ottawaCenter)
                        setZoom(10.0f)
                        setOnAttributionClickListener {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.openstreetmap.org/copyright".toUri(),
                            )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RouteControls(
                    mapView = mapView,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun RouteControls(
    mapView: OpenMapView?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pathPrepper = remember { PathPrepper() }

    var startAddress by remember { mutableStateOf("") }
    var destAddress by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Enter start and destination") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Route Planner",
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = startAddress,
            onValueChange = { startAddress = it },
            label = { Text("Start address") },
            placeholder = { Text("e.g. Ottawa, ON") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = destAddress,
            onValueChange = { destAddress = it },
            label = { Text("Destination") },
            placeholder = { Text("e.g. Toronto, ON") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                if (!Keep.isOpenRouteServiceKeyValid()) {
                    Toast.makeText(context, "Set your ORS API key in Keep.kt", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (startAddress.isBlank() || destAddress.isBlank()) {
                    Toast.makeText(context, "Enter both addresses", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                statusText = "Geocoding addresses..."

                scope.launch {
                    try {
                        // Geocode both addresses (on IO thread)
                        val startCoord = withContext(Dispatchers.IO) {
                            pathPrepper.geocode(startAddress)
                        }
                        val destCoord = withContext(Dispatchers.IO) {
                            pathPrepper.geocode(destAddress)
                        }

                        if (startCoord == null || destCoord == null) {
                            statusText = "Could not find one or both addresses"
                            isLoading = false
                            return@launch
                        }

                        // Store coordinates for the route request
                        Keep.startPos = startCoord
                        Keep.destPos = destCoord
                        statusText = "Fetching route..."

                        // Get route from ORS Directions API
                        val result: GeoJsonResult? = withContext(Dispatchers.IO) {
                            pathPrepper.getRoute()
                        }

                        if (result != null && result.polylines.isNotEmpty()) {
                            // Draw route on map
                            mapView?.let { map ->
                                map.clearPolylines()
                                for (polyline in result.polylines) {
                                    // Re-create with a visible blue color and thicker width
                                    val styledPolyline = Polyline(
                                        points = polyline.points,
                                        strokeColor = Color(0xFF1976D2),
                                        strokeWidth = 8f,
                                    )
                                    map.addPolyline(styledPolyline)
                                }
                                // Center map on start point
                                map.setCenter(LatLng(startCoord.latitude, startCoord.longitude))
                                map.setZoom(8.0f)
                            }
                            statusText = "Route found! ${result.polylines.first().points.size} points"
                        } else {
                            statusText = "No route found"
                        }
                    } catch (e: Exception) {
                        statusText = "Error: ${e.message}"
                        e.printStackTrace()
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Loading..." else "Get Route")
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}