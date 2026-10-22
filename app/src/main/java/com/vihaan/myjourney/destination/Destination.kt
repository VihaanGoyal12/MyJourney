package com.vihaan.myjourney.destination

/**
 * Represents a resolved destination candidate or user selection.
 *
 * In Java terms: A standard POJO/Record holding the destination metadata.
 */
data class Destination(
    val placeId: String,
    val name: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)
