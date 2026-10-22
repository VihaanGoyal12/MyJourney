package com.vihaan.myjourney.destination

/**
 * Represents the distinct states of the destination search workflow.
 *
 * In Java terms: A sealed hierarchy/enum with associated payload representing mutually exclusive states.
 */
sealed interface DestinationSearchState {
    data object Idle : DestinationSearchState
    data object Loading : DestinationSearchState
    data class Success(val results: List<Destination>) : DestinationSearchState
    data object Empty : DestinationSearchState
    data class Error(val message: String) : DestinationSearchState
}
