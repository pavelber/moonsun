package org.bernshtam.moonandsun

import com.google.android.gms.maps.model.LatLng
import net.time4j.calendar.astro.GeoLocation

class MyLocation(private val latitude: Double, private val longitude: Double) :
    GeoLocation {

    constructor(p: LatLng) : this(p.latitude, p.longitude)

    override fun getAltitude(): Int = 0


    override fun getLatitude(): Double = latitude

    override fun getLongitude(): Double = longitude


}