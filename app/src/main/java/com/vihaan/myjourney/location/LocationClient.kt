package com.vihaan.myjourney.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Encapsulates location retrieval using Google Play Services FusedLocationProviderClient.
 *
 * In Java terms: This is a helper/service class that wraps the underlying Google Play Services API
 * and provides a callback-based method to fetch the device's coordinates.
 */
class LocationClient(context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Requests the current location with high accuracy.
     * Note: Callers must ensure location permissions are granted before invoking this method.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onSuccess: (Location?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location)
            } else {
                // If getCurrentLocation is null (common on freshly started emulators before GPS lock),
                // attempt to check last known location as a fallback.
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        onSuccess(lastLocation)
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }
}
