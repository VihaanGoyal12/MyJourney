package com.vihaan.myjourney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vihaan.myjourney.destination.Destination
import com.vihaan.myjourney.destination.DestinationSearchState
import com.vihaan.myjourney.destination.GeoapifyClient
import com.vihaan.myjourney.location.LocationClient
import com.vihaan.myjourney.location.LocationState
import com.vihaan.myjourney.ui.theme.MyJourneyTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyJourneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InitialScreen()
                }
            }
        }
    }
}

@Composable
fun InitialScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Location Management (Milestone 2 - Preserved)
    val locationClient = remember { LocationClient(context) }
    var locationState by remember { mutableStateOf<LocationState>(LocationState.Idle) }

    // Destination Management (Milestone 3)
    val geoapifyClient = remember { GeoapifyClient() }
    var searchQuery by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<DestinationSearchState>(DestinationSearchState.Idle) }
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }

    fun requestLocation() {
        locationState = LocationState.Loading
        locationClient.getCurrentLocation(
            onSuccess = { location ->
                if (location != null) {
                    locationState = LocationState.Success(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    locationState = LocationState.Error(
                        "Unable to obtain location.\nEnsure GPS/Location is enabled on the device."
                    )
                }
            },
            onFailure = { exception ->
                locationState = LocationState.Error(
                    exception.localizedMessage ?: "Failed to acquire location."
                )
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            requestLocation()
        } else {
            locationState = LocationState.PermissionDenied
        }
    }

    LaunchedEffect(Unit) {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            requestLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Debounced Destination Search
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (selectedDestination != null && selectedDestination?.name == trimmed) {
            return@LaunchedEffect
        }

        if (trimmed.length < 3) {
            searchState = DestinationSearchState.Idle
            return@LaunchedEffect
        }

        delay(400) // Debounce delay
        searchState = DestinationSearchState.Loading

        val currentLat = (locationState as? LocationState.Success)?.latitude
        val currentLon = (locationState as? LocationState.Success)?.longitude

        val result = geoapifyClient.searchDestinations(
            query = trimmed,
            proximityLat = currentLat,
            proximityLon = currentLon
        )

        searchState = result.fold(
            onSuccess = { list ->
                if (list.isEmpty()) DestinationSearchState.Empty else DestinationSearchState.Success(list)
            },
            onFailure = { error ->
                DestinationSearchState.Error(error.localizedMessage ?: "Search failed")
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Text(
            text = "MyJourney",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your companion for the journey.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 1: Current Location (Device GPS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "1. CURRENT LOCATION (GPS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = locationState) {
                    is LocationState.Idle, is LocationState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "  Getting your location...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is LocationState.Success -> {
                        Text(
                            text = "Latitude: ${state.latitude}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Longitude: ${state.longitude}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { requestLocation() }) {
                            Text("Refresh GPS Location")
                        }
                    }

                    is LocationState.PermissionDenied -> {
                        Text(
                            text = "Location permission was denied.\nMyJourney needs location access to know where your journey begins.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) {
                            Text("Grant Permission")
                        }
                    }

                    is LocationState.Error -> {
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { requestLocation() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: Destination Search
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "2. DESTINATION SEARCH (GEOAPIFY)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery
                        if (selectedDestination != null && newQuery != selectedDestination?.name) {
                            selectedDestination = null
                        }
                    },
                    label = { Text("Search city/place (e.g. Meerut, Mumbai)") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedDestination = null
                                searchState = DestinationSearchState.Idle
                            }) {
                                Text("Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search Status & Suggestions
                when (val state = searchState) {
                    is DestinationSearchState.Loading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "  Searching Geoapify...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    is DestinationSearchState.Success -> {
                        Text(
                            text = "Tap a destination below to select:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                state.results.forEachIndexed { index, destination ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedDestination = destination
                                                searchQuery = destination.name
                                                searchState = DestinationSearchState.Idle
                                                keyboardController?.hide()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = destination.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = destination.formattedAddress,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "Coords: ${destination.latitude}, ${destination.longitude}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (index < state.results.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is DestinationSearchState.Empty -> {
                        Text(
                            text = "No destinations found for \"$searchQuery\".",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is DestinationSearchState.Error -> {
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is DestinationSearchState.Idle -> {
                        if (searchQuery.isNotEmpty() && searchQuery.length < 3 && selectedDestination == null) {
                            Text(
                                text = "Type at least 3 characters to search...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Selected Destination Display
                selectedDestination?.let { dest ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "✓ SELECTED DESTINATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dest.formattedAddress,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Latitude: ${dest.latitude}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Longitude: ${dest.longitude}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Required Geoapify Attribution
        Text(
            text = "Powered by Geoapify",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialScreenPreview() {
    MyJourneyTheme {
        InitialScreen()
    }
}
