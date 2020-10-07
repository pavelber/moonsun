package org.bernshtam.moonandsun

import android.widget.TextView
import net.time4j.Moment
import net.time4j.calendar.astro.MoonPhase

class ExplanationContainer(
    private val explanation: TextView,
    private val sunrise: String,
    private val sunset: String,
    private val moonrise: String,
    private val moonset: String,
    private val moonillu: String
) {


    fun showData(
        sunrise: Moment?,
        sunset: Moment?,
        moonrise: Moment?,
        moonset: Moment?
    ) {

        val momentInADay = moonrise ?: moonset ?: sunrise ?: sunset
        val moonIllumination = (MoonPhase.getIllumination(momentInADay) * 100).toInt()
        val sunriseStr = sunrise?.let{TIME.format(sunrise.toLocalTimeDate())}
        val sunsetStr = sunset?.let{TIME.format(sunset.toLocalTimeDate())}
        val moonriseStr = moonrise?.let{TIME.format(moonrise.toLocalTimeDate())}
        val moonsetStr = moonset?.let{TIME.format(moonset.toLocalTimeDate())}
        explanation.text =
            createText(sunriseStr, sunsetStr, moonriseStr, moonsetStr, moonIllumination)
    }

    private fun createText(
        sunriseStr: String?,
        sunsetStr: String?,
        moonriseStr: String?,
        moonsetStr: String?,
        moonIllumination: Int
    ): String {
        val s = mutableListOf<String>()
        sunriseStr?.apply { s.add("$sunrise: $sunriseStr") }
        sunsetStr?.apply { s.add("$sunset: $sunsetStr") }
        moonriseStr?.apply { s.add("$moonrise: $moonriseStr") }
        moonsetStr?.apply { s.add("$moonset: $moonsetStr") }
        s.add("$moonillu: $moonIllumination %")
        return  s.joinToString()
    }
}