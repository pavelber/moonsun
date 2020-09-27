package org.bernshtam.moonandsun

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.grum.geocalc.Coordinate
import com.grum.geocalc.EarthCalc
import com.grum.geocalc.Point

import net.time4j.Moment
import net.time4j.PlainDate
import net.time4j.calendar.astro.*
import net.time4j.engine.CalendarDate
import net.time4j.engine.ChronoFunction
import net.time4j.tz.ZonalOffset

class MapContainer(val mMap: GoogleMap, private val explanationContainer: ExplanationContainer) : GoogleMap.OnMapClickListener {
    init {
        mMap.setOnMapClickListener(this)
    }
    private val markers: MutableList<Polyline> = mutableListOf()

    override fun onMapClick(p0: LatLng?) {

        p0?.apply {
            val today = PlainDate.nowInSystemTime()
            val geolocation = MyLocation(p0)
            val here = SolarTime.ofLocation(latitude, longitude)
            val lunarTime = LunarTime.ofLocation(
                ZonalOffset.ofTotalSeconds(TZ.rawOffset / 1000),
                latitude,
                longitude
            )
            val moonlight = lunarTime.on(today)
            val moonrise = moonlight.moonrise()
            val moonset = moonlight.moonset()
            val sunrise: ChronoFunction<CalendarDate, Moment> = here.sunrise()
            val sunset: ChronoFunction<CalendarDate, Moment> = here.sunset()

            val sunriseMoment = sunrise.apply(today)
            val sunsetMoment = sunset.apply(today)

            removeMarkers()
            showSun(sunriseMoment, Color.rgb(255, 255, 0), geolocation)
            showSun(sunsetMoment, Color.rgb(255, 0, 0), geolocation)
            showMoon(moonrise, Color.rgb(93, 100, 117), geolocation)
            showMoon(moonset, Color.rgb(52, 61, 82), geolocation)


            explanationContainer.showData(sunrise, sunset, moonrise, moonset)
        }

    }

    private fun showSun(moment: Moment?, c: Int, loc: GeoLocation) {
        moment?.apply {
            markers.add(
                showSunLine(
                    moment,
                    c,
                    loc
                )
            )
        }
    }

    private fun showMoon(moment: Moment?, c: Int, loc: GeoLocation) {
        moment?.apply {
            markers.add(
                showMoonLine(
                    moment,
                    c,
                    loc
                )
            )
        }
    }

    private fun showSunLine(m: Moment, c: Int, location: GeoLocation): Polyline {

        val sunPosition = SunPosition.at(m, location)
        val azimuth = sunPosition.azimuth
        return showLine(azimuth, c, location)
    }

    private fun showMoonLine(m: Moment, c: Int, location: GeoLocation): Polyline {

        val moonPosition = MoonPosition.at(m, location)
        val azimuth = moonPosition.azimuth
        return showLine(azimuth, c, location)
    }

    private fun showLine(azimuth: Double, c: Int, location: GeoLocation): Polyline {
        val point = EarthCalc.pointAt(
            Point.at(
                Coordinate.fromDegrees(location.latitude),
                Coordinate.fromDegrees(location.longitude)
            ),
            azimuth, 12000.0
        )

        return mMap.addPolyline(
            PolylineOptions().add(LatLng(location.latitude, location.longitude)).add(
                LatLng(point.latitude, point.longitude)
            ).color(c)
        )
    }


    private fun removeMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }
}