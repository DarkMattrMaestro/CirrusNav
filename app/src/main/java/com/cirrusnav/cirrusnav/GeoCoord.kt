package com.cirrusnav.cirrusnav

/**
 * Represents a geographic coordinate with longitude and latitude.
 */
data class GeoCoord(
    val longitude: Double,
    val latitude: Double,
) {
    override fun toString(): String {
        return "$longitude,$latitude"
    }
}