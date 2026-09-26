package com.cirrusnav.cirrusnav

class GeoCoord {
    val longitude: Double? = null
    val latitude: Double? = null

    fun isValid(): Boolean {
        return longitude != null && latitude != null
    }

    fun toURLString(): String {
        return "$longitude,$latitude"
    }
}