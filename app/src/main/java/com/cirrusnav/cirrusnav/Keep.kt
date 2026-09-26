package com.cirrusnav.cirrusnav

object Keep {
    val openRouteServiceKey = "";

    fun isOpenRouteServiceKeyValid(): Boolean {
        // TODO: Improve sanitization
        if (openRouteServiceKey.contains("&")) {
            return false
        }
        return !openRouteServiceKey.isEmpty()
    }

    val destPos: GeoCoord = GeoCoord()
    val startPos: GeoCoord = GeoCoord()
}