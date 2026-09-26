package com.cirrusnav.cirrusnav

object Keep {
    val openRouteServiceKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjMyYTVjZWRjY2E5YjQ1NmQ4ZjA4MGJiYTJiZDhiYjU2IiwiaCI6Im11cm11cjY0In0=";

    fun isOpenRouteServiceKeyValid(): Boolean {
        // TODO: Improve sanitization
        if (openRouteServiceKey.contains("&")) {
            return false
        }
        return !openRouteServiceKey.isEmpty()
    }

    var destPos: GeoCoord = GeoCoord()
    var startPos: GeoCoord = GeoCoord()
}