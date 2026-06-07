package danilovl.calendar.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import danilovl.calendar.R
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URL
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: String? = null,
    onLocationSelected: (String) -> Unit = {},
    onBack: () -> Unit,
    readOnly: Boolean = false
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    
    val sharedPrefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
    val lastLat = sharedPrefs.getFloat("last_ip_lat", 0f).toDouble()
    val lastLon = sharedPrefs.getFloat("last_ip_lon", 0f).toDouble()
    val cachedLocation = if (lastLat != 0.0) GeoPoint(lastLat, lastLon) else null

    val labelLocation = stringResource(R.string.label_location)
    val pointSelectedLabel = stringResource(R.string.location_point_selected)

    var markerPosition by remember { 
        val geoPrefix = "geo:"
        val pos = if (initialLocation?.startsWith(geoPrefix) == true) {
            val coordsPart = initialLocation.substring(geoPrefix.length).split("|")[0]
            val coords = coordsPart.split(",")
            if (coords.size == 2) {
                val lat = coords[0].trim().toDoubleOrNull()
                val lon = coords[1].trim().toDoubleOrNull()
                if (lat != null && lon != null) GeoPoint(lat, lon) else null
            } else null
        } else null
        mutableStateOf(pos)
    }

    var isLocating by remember { mutableStateOf(markerPosition == null && cachedLocation == null) }
    
    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            controller.setZoom(if (markerPosition != null) 15.0 else 12.0)
            
            val initialCenter = markerPosition ?: cachedLocation
            if (initialCenter != null) {
                controller.setCenter(initialCenter)
            }
            
            markerPosition?.let { pos ->
                val marker = Marker(this).apply {
                    position = pos
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = labelLocation
                }
                overlays.add(marker)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (markerPosition == null) {
            withContext(Dispatchers.IO) {
                val services = listOf(
                    "https://ipwho.is/",
                    "https://ipapi.co/json/",
                    "https://freeipapi.com/api/json"
                )
                
                for (service in services) {
                    try {
                        val connection = URL(service).openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        connection.setRequestProperty("Accept", "application/json")
                        
                        val response = connection.getInputStream().bufferedReader().use { it.readText() }
                        AppLog.d("LocationPicker", "Response from $service: $response")
                        val json = JSONObject(response)
                        
                        val city = json.optString("city", json.optString("city_name", "Unknown"))
                        AppLog.i("LocationPicker", "Detected location: $city")
                        
                        val lat = if (json.has("latitude")) json.optDouble("latitude", 0.0) else json.optDouble("lat", 0.0)
                        val lon = if (json.has("longitude")) json.optDouble("longitude", 0.0) else json.optDouble("lon", 0.0)
                        
                        if (lat != 0.0 && lon != 0.0) {
                            withContext(Dispatchers.Main) {
                                val newPoint = GeoPoint(lat, lon)
                                if (markerPosition == null) {
                                    mapView.controller.setZoom(12.0)
                                    mapView.controller.animateTo(newPoint)
                                }
                                sharedPrefs.edit()
                                    .putFloat("last_ip_lat", lat.toFloat())
                                    .putFloat("last_ip_lon", lon.toFloat())
                                    .apply()
                                isLocating = false
                            }
                            break
                        }
                    } catch (e: Exception) {
                        AppLog.e("LocationPicker", "Geocoding failed", e)
                    }
                }
                withContext(Dispatchers.Main) {
                    isLocating = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    val topAppBarTitle = if (readOnly) {
        stringResource(R.string.map_view_title)
    } else {
        stringResource(R.string.select_location)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topAppBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!readOnly) {
                        val scope = rememberCoroutineScope()
                        TextButton(
                            onClick = {
                                markerPosition?.let { pos ->
                                    scope.launch {
                                        val address = withContext(Dispatchers.IO) {
                                            try {
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                @Suppress("DEPRECATION")
                                                val addresses = geocoder.getFromLocation(pos.latitude, pos.longitude, 1)
                                                if (!addresses.isNullOrEmpty()) {
                                                    addresses[0].getAddressLine(0)
                                                } else null
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        val finalAddress = address ?: pointSelectedLabel
                                        onLocationSelected("geo:${pos.latitude},${pos.longitude}|$finalAddress")
                                    }
                                }
                            },
                            enabled = markerPosition != null
                        ) {
                            Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = XiaomiBg)
            )
        },
        floatingActionButton = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                FloatingActionButton(
                    onClick = {
                        val locationOverlay = mapView.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()
                        locationOverlay?.let {
                            val myLoc = it.myLocation
                            if (myLoc != null) {
                                mapView.controller.animateTo(myLoc)
                                mapView.controller.setZoom(16.0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                if (readOnly) return false
                                markerPosition = p
                                overlays.filterIsInstance<Marker>().forEach { overlays.remove(it) }
                                val marker = Marker(this@apply).apply {
                                    position = p
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = labelLocation
                                }
                                overlays.add(marker)
                                invalidate()
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean {
                                if (readOnly) return false
                                return singleTapConfirmedHelper(p)
                            }
                        }
                        overlays.add(MapEventsOverlay(eventsReceiver))

                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
                                enableMyLocation()
                                runOnFirstFix {
                                    post {
                                        controller.animateTo(myLocation)
                                    }
                                }
                            }
                            overlays.add(locationOverlay)
                        }
                    }
                },
                update = { view ->
                }
            )

            if (markerPosition == null && !readOnly) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ) {
                    Text(
                        stringResource(R.string.map_instruction),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
