package com.cirrusnav.cirrusnav

class GeoCoord {
    var longitude: Double? = null
    var latitude: Double? = null

    fun isValid(): Boolean {
        return longitude != null && latitude != null
    }

    fun toURLString(): String {
        return "$longitude,$latitude"
    }
}