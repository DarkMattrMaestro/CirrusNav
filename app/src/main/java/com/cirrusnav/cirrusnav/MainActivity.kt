package com.cirrusnav.cirrusnav

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.preference.PreferenceFragmentCompat
import de.afarber.openmapview.Circle
import de.afarber.openmapview.CircleOptions
import de.afarber.openmapview.GeoJsonResult
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Polygon
import de.afarber.openmapview.Polyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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
    var openRouteServiceKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Enter start and destination") }

    var isSettingsOpen by remember { mutableStateOf(false) }

    if (isSettingsOpen) {
        Dialog(
            onDismissRequest = { isSettingsOpen = false }
        ) {
            Box(
                Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Settings (auto-saves)",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    OutlinedTextField(
                        value = openRouteServiceKey,
                        onValueChange = { openRouteServiceKey = it },
                        label = { Text("OpenRouteService API Key") },
                        placeholder = { Text("fh4397...4f7") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            Keep.openRouteServiceKey = openRouteServiceKey
                            isSettingsOpen = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Exit Settings")
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow {
            Text(
                text = "Route Planner",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            IconButton(
                onClick = { isSettingsOpen = true },
                enabled = true,
                colors = IconButtonDefaults.iconButtonColors(),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(Dp(24f)).align(Alignment.CenterVertically),
                    tint = if (Keep.isOpenRouteServiceKeyValid()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
        }

        OutlinedTextField(
            value = startAddress,
            onValueChange = { startAddress = it },
            label = { Text("Start address") },
            placeholder = { Text("e.g. Ottawa, ON") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = Keep.isOpenRouteServiceKeyValid()
        )

        OutlinedTextField(
            value = destAddress,
            onValueChange = { destAddress = it },
            label = { Text("Destination") },
            placeholder = { Text("e.g. Toronto, ON") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = Keep.isOpenRouteServiceKeyValid()
        )

        Button(
            onClick = {
                if (!Keep.isOpenRouteServiceKeyValid()) {
                    Toast.makeText(context, "Set your ORS API key in settings", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (startAddress.isBlank() || destAddress.isBlank()) {
                    Toast.makeText(context, "Enter both addresses", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                mapView?.clearPolygons()

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
                        val result: RouteResult? = withContext(Dispatchers.IO) {
                            pathPrepper.getRoute()
                        }

                        if (result != null && result.polylines.isNotEmpty()) {
                            val durationHours = (result.durationSeconds / 3600).toInt()
                            val durationMinutes = ((result.durationSeconds % 3600) / 60).toInt()
                            val distanceKm = result.distanceMeters / 1000.0
                            val distanceStr = String.format(java.util.Locale.US, "%.1f km", distanceKm)
                            
                            var timeStr = ""
                            if (durationHours > 0) timeStr += "${durationHours}h "
                            timeStr += "${durationMinutes}m"
                            
                            // Draw route on map
                            mapView?.let { map ->
                                map.clearPolylines()
                                map.clearMarkers() // Clear old weather pins

                                for (polyline in result.polylines) {
                                    val styledPolyline = Polyline(
                                        points = polyline.points,
                                        strokeColor = Color(0xFF1976D2),
                                        strokeWidth = 8f,
                                    )
                                    map.addPolyline(styledPolyline)
                                }
                                map.setCenter(LatLng(startCoord.latitude, startCoord.longitude))
                                map.setZoom(8.0f)
                            }

                            // Sample path regularly
                            statusText = "Fetching weather data..."
                            samplePath(mapView!!)
                            
//                            // Add Weather Markers
//                            if (Keep.weatherApiKey.isNotBlank()) {
//                                statusText = "Fetching weather for route..."
//                                val weatherPrepper = WeatherPrepper()
//                                val points = result.polylines.first().points
//
//                                // Sample checkpoints: 33%, 66%, and Destination (100%)
//                                val fractions = listOf(0.33, 0.66, 1.0)
//
//                                for (fraction in fractions) {
//                                    val index = ((points.size - 1) * fraction).toInt()
//                                    val point = points[index]
//                                    val etaSeconds = result.durationSeconds * fraction
//
//                                    val weather = withContext(Dispatchers.IO) {
//                                        weatherPrepper.getWeatherAtETA(point.latitude, point.longitude, etaSeconds)
//                                    }
//
//                                    if (weather != null) {
//                                        val etaHoursStr = String.format(java.util.Locale.US, "%.1fh", weather.etaHours)
//                                        val marker = de.afarber.openmapview.Marker(
//                                            position = point,
//                                            title = "${weather.iconEmoji} ${weather.tempC}°C",
//                                            snippet = "+$etaHoursStr (${weather.condition})"
//                                        )
//                                        mapView.addMarker(marker)
//                                    }
//                                }
//                            } else {
//                                Toast.makeText(context, "Paste WeatherAPI key in Keep.kt for weather pins", Toast.LENGTH_LONG).show()
//                            }

                            statusText = "Route found! $timeStr ($distanceStr)"
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
            enabled = !isLoading && Keep.isOpenRouteServiceKeyValid(),
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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
suspend fun samplePath(mapView: OpenMapView, ) {
    val isSmoothed: Boolean = true

    val samples: ArrayList<Int> = ArrayList<Int>()
    var lastSampleDist: Float = 0f
    var sampleStep: Int = 1000 // step in meters
    val maxStep: Int = 20000 // step in meters
    var distTravelled: Float = 0f
    var lastLatLng: LatLng? = null

    // Sample by distance
    val features = Keep.path?.getJSONArray("features")
    val geometry = features?.getJSONObject(0)?.getJSONObject("geometry")
    val coordinates = geometry?.getJSONArray("coordinates")
    for (i in 0 until coordinates!!.length()) {
        val coord: JSONArray = coordinates.getJSONArray(i)
        val latLng: LatLng = LatLng(coord.get(1) as Double, coord.get(0) as Double)
        // Cumulate distance travelled
        if (lastLatLng != null) {
            val distanceRes: FloatArray = FloatArray(1)
            Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, distanceRes)
            distTravelled += distanceRes[0]
        }
        lastLatLng = latLng

        // Check if key point for measurement
        if (lastSampleDist + sampleStep <= distTravelled) {
            lastSampleDist += sampleStep
            sampleStep = min((sampleStep * 4f).toInt(), maxStep)
            samples.add(i)
        }
    }

//    Log.i("MainActivity", "Completed sampling key points [${samples.size}]: ${samples}")

    val weatherAtSample: HashMap<Int, WeatherResult> = HashMap<Int, WeatherResult>()
    // Collect weather data samples
    for (i: Int in samples) {
        val coord: JSONArray = coordinates.getJSONArray(i)
        val sample: LatLng = LatLng(coord.get(1) as Double, coord.get(0) as Double)
//        Log.i("MainActivity", "Pos at ${sample}")

        val travelTime: Double = PathPrepper().getRoute(Keep.startPos, sample, false)!!.durationSeconds
        val weatherRes: WeatherResult? = WeatherPrepper().getWeatherAtETA(sample.latitude, sample.longitude, travelTime)
        weatherAtSample[i] = weatherRes?: WeatherResult(0.0,"","",0.0,0.0)
    }

//    Log.i("MainActivity", "Completed sampling weather data [${weatherAtSample.size}]: ${weatherAtSample}")

    // Interpolate weather data
//    val rainFactor: Double = 500.0
    val borderPoints: ArrayList<LatLng> = ArrayList<LatLng>()
    if (isSmoothed) {
        val smoothStep: Int = 5000
        var travelled: Float = 0f
        var lastSmooth: Float = 0f
        for (i in 0 until coordinates.length() - 1) {
//    for (i: Int in samples) {
            val coord: JSONArray = coordinates.getJSONArray(i)
            val latLng: LatLng = LatLng(coord.get(1) as Double, coord.get(0) as Double)
            val nextCoord: JSONArray = coordinates.getJSONArray(i + 1)
            val nextLatLng: LatLng = LatLng(nextCoord.get(1) as Double, nextCoord.get(0) as Double)
//        Log.i("MainActivity", "Iterating: ${latLng}")

            val distanceResp: FloatArray = FloatArray(1)
            Location.distanceBetween(
                nextLatLng.latitude,
                nextLatLng.longitude,
                latLng.latitude,
                latLng.longitude,
                distanceResp
            )
            travelled += distanceResp[0]
            if (travelled > lastSmooth + smoothStep) {
                lastSmooth += smoothStep
            } else {
                continue
            }
//
            var below: Int = 0
            var belowDist: Int = Int.MAX_VALUE
            var above: Int = 0
            var aboveDist: Int = Int.MAX_VALUE
            for (j in 0 until samples.size) {
                Log.i(
                    "MainActivity",
                    "Iter: ${i} ${below} ${belowDist}; ${above} ${aboveDist}; ${j} ${samples[j]}"
                )
                if (samples[j] > i && samples[j] - i < aboveDist) {
                    aboveDist = samples[j] - i
                    above = j
                }
                if (samples[j] < i && i - samples[j] < aboveDist) {
                    belowDist = i - samples[j]
                    below = j
                }
            }
//
//        Log.i("MainActivity", "Done: ${i} -> ${samples[below]} ${belowDist}; ${samples[above]} ${aboveDist}")
//
            val distanceRes: FloatArray = FloatArray(1)
            Location.distanceBetween(
                coordinates.getJSONArray(samples[below]).getDouble(1),
                coordinates.getJSONArray(samples[below]).getDouble(0),
                coord.getDouble(1),
                coord.getDouble(0),
                distanceRes
            )
            val distFromBelow: Float = distanceRes[0]
//
            Location.distanceBetween(
                coordinates.getJSONArray(samples[above]).getDouble(1),
                coordinates.getJSONArray(samples[above]).getDouble(0),
                coord.getDouble(1),
                coord.getDouble(0),
                distanceRes
            )
            val distFromAbove: Float = distanceRes[0]
//
//        Log.i("MainActivity", "${samples}")
//        Log.i("MainActivity", "in sample ${below} ${above}")
//        Log.i("MainActivity", "sample ${samples[below]} ${samples[above]}")
//        Log.i("MainActivity", "${belowDist} ${aboveDist}")
//        Log.i("MainActivity", "${weatherAtSample}")
//        Log.i("MainActivity", "${weatherAtSample[samples[below]]} ${weatherAtSample[samples[above]]}")
//        Log.i("MainActivity", "precip ${weatherAtSample[samples[below]]!!.precip_mm} ${weatherAtSample[samples[above]]!!.precip_mm}")
//        val distPoint: Double = rainFactor * (
//                weatherAtSample[samples[below]]!!.precip_mm * distFromBelow
//                + weatherAtSample[samples[above]]!!.precip_mm * distFromAbove
//                ) / (distFromBelow + distFromAbove);
//


            val distPoint: Double = 0.02 * (
                    weatherAtSample[samples[below]]!!.precip_mm * distFromBelow
                            + weatherAtSample[samples[above]]!!.precip_mm * distFromAbove
                    ) / (distFromBelow + distFromAbove)

            val latDiff: Double = nextLatLng.latitude - latLng.latitude;
            val longDiff: Double = nextLatLng.longitude - latLng.longitude;
            val length: Double = sqrt(latDiff * latDiff + longDiff * longDiff);
            val uLat: Double = latDiff / length;
            val uLong: Double = longDiff / length;

            val newLat1: Double = latLng.latitude + (distPoint / 2) * uLong;
            val newLong1: Double = latLng.longitude - (distPoint / 2) * uLat;

            val newLat2: Double = latLng.latitude - (distPoint / 2) * uLong;
            val newLong2: Double = latLng.longitude + (distPoint / 2) * uLat;

            borderPoints.addFirst(LatLng(newLat1, newLong1))
            borderPoints.addLast(LatLng(newLat2, newLong2))
        }
    } else {
        for (i: Int in samples) {
            val coord: JSONArray = coordinates.getJSONArray(i)
            val latLng: LatLng = LatLng(coord.get(1) as Double, coord.get(0) as Double)
            val nextCoord: JSONArray = coordinates.getJSONArray(i + 1)
            val nextLatLng: LatLng = LatLng(nextCoord.get(1) as Double, nextCoord.get(0) as Double)

            val distPoint: Double = 0.02 * weatherAtSample[i]!!.precip_mm

            val latDiff: Double = nextLatLng.latitude - latLng.latitude;
            val longDiff: Double = nextLatLng.longitude - latLng.longitude;
            val length: Double = sqrt(latDiff * latDiff + longDiff * longDiff);
            val uLat: Double = latDiff / length;
            val uLong: Double = longDiff / length;

            val newLat1: Double = latLng.latitude + (distPoint / 2) * uLong;
            val newLong1: Double = latLng.longitude - (distPoint / 2) * uLat;

            val newLat2: Double = latLng.latitude - (distPoint / 2) * uLong;
            val newLong2: Double = latLng.longitude + (distPoint / 2) * uLat;

            borderPoints.addFirst(LatLng(newLat1, newLong1))
            borderPoints.addLast(LatLng(newLat2, newLong2))
        }
    }

    Log.i("MainActivity", "borderPoints length: ${borderPoints.size}")

    mapView.addPolygon(
        Polygon(
            points = borderPoints,
            strokeColor = Color(1f, 0f, 0f, 0.0f),
            fillColor = Color(1f, 0f, 0f, 0.5f),
            clickable = false,
        )
    )

    mapView.addPolygon(Polygon(
        points = borderPoints,
    ))

//    for (i: Int in samples) {
//        val coord: JSONArray = coordinates.getJSONArray(i)
//        val sample: LatLng = LatLng(coord.get(1) as Double, coord.get(0) as Double)
//        Log.i("MainActivity", "Samples[i]: ${i}")
//        Log.i("MainActivity", "Circles Pos at: ${(rainFactor * weatherAtSample[i]!!.precip_mm).toFloat()}")
//        mapView.addCircle(
//            Circle(
//                center = sample,
//                radius = (rainFactor * weatherAtSample[i]!!.precip_mm).toFloat(),  // Radius in meters
//                strokeColor = Color.Red,
//                strokeWidth = 6f,
//                fillColor = Color.Red,
//                clickable = true,
//                zIndex = 1.5f,
//                tag = "Kotlin Style Circle"
//            )
//        )
//    }
}