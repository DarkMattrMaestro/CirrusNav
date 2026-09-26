package com.cirrusnav.cirrusnav

import android.R
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.PredefinedTileProviders
import de.afarber.openmapview.TileOverlay

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

data class OverlayState(
    val name: String,
    val overlay: TileOverlay,
    val attribution: String,
    var isVisible: Boolean = false,
    var transparency: Float = 0.5f,
)

@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }

    val bochumCenter = LatLng(45.4209, -75.6899)

    // Show initial instruction toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "AAAAAAAAAAAAAAAA",
            Toast.LENGTH_LONG,
        ).show()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(bochumCenter)
                        setZoom(12.0f)
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
                    .width(250.dp)
                    .fillMaxHeight(),
            ) {
                OverlayControls(
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
                        setCenter(bochumCenter)
                        setZoom(12.0f)
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
                    .fillMaxWidth(),
            ) {
                OverlayControls(
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun OverlayControls(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Title of lower section",
            style = MaterialTheme.typography.titleMedium,
        )

        PrepPath()
    }

}


@Composable
fun PrepPath() {
    var clicked by remember { mutableStateOf("Click Me") }

    Button(
        onClick = {
            clicked = "Clicked"
        }
    )
    {
        Text(text = clicked)
    }

}


//Button test
@Composable
fun ChangingTextButton() {

    var buttonText by remember { mutableStateOf("Click Me") }


    Button(
        onClick = {

            buttonText = "Clicked!"
        }
    ) {
        Text(text = buttonText)
    }
}