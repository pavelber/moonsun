package org.bernshtam.moonandsun

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
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
import java.time.LocalDate


class MapContainer(
    val mMap: GoogleMap,
    private val explanationContainer: ExplanationContainer,
    private val date: LocalDate
) :
    GoogleMap.OnMapClickListener {
    init {
        mMap.setOnMapClickListener(this)
    }

    private val markers: MutableList<Polyline> = mutableListOf()

    override fun onMapClick(p0: LatLng?) {

        p0?.apply {
            val today = PlainDate.of(date.year, date.dayOfYear)
            val geolocation = MyLocation(p0)
            val here = SolarTime.ofLocation(latitude, longitude)
            val lunarTime = LunarTime.ofLocation(
                ZonalOffset.ofTotalSeconds(TZ.rawOffset / 1000),
                latitude,
                longitude
            )
            val moonlight = lunarTime.on(today)
            val moonriseMoment = moonlight.moonrise()
            val moonsetMoment = moonlight.moonset()
            val sunrise: ChronoFunction<CalendarDate, Moment> = here.sunrise()
            val sunset: ChronoFunction<CalendarDate, Moment> = here.sunset()

            val sunriseMoment = sunrise.apply(today)
            val sunsetMoment = sunset.apply(today)

            removeMarkers()
            showSun(sunriseMoment, Color.rgb(237, 184, 121), geolocation)
            showSun(sunsetMoment, Color.rgb(224, 123, 57), geolocation)
            showMoon(moonriseMoment, Color.rgb(105, 189, 210), geolocation)
            showMoon(moonsetMoment, Color.rgb(25, 121, 169), geolocation)


            explanationContainer.showData(
                sunriseMoment,
                sunsetMoment,
                moonriseMoment,
                moonsetMoment
            )
        }

    }

    fun rotateMap(bearing: Float) {
        val currentPlace = CameraPosition.Builder()
            .target(mMap.cameraPosition.target).zoom(mMap.cameraPosition.zoom)
            .bearing(bearing).build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace))
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
            azimuth, 120000.0
        )

        return mMap.addPolyline(
            PolylineOptions().add(LatLng(location.latitude, location.longitude)).add(
                LatLng(point.latitude, point.longitude)
            ).color(c)
        )
    }


    fun removeMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }
}