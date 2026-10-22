package com.vihaan.myjourney.location

/**
 * Represents the distinct states of obtaining the user's location.
 *
 * In Java terms: A sealed interface is like an enum with associated data,
 * or a restricted class hierarchy where all subtypes are known at compile-time.
 */
sealed interface LocationState {
    data object Idle : LocationState
    data object Loading : LocationState
    data object PermissionDenied : LocationState
    data class Success(val latitude: Double, val longitude: Double) : LocationState
    data class Error(val message: String) : LocationState
}
