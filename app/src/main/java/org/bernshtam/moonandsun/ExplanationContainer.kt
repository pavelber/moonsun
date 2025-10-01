package org.bernshtam.moonandsun

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import net.time4j.Moment
import net.time4j.PlainDate
import net.time4j.engine.CalendarDate
import net.time4j.engine.ChronoFunction
import java.text.SimpleDateFormat
import java.util.*

class ExplanationContainer(
    private val explanationTextView: TextView,
    private val sunriseLabel: String,
    private val sunsetLabel: String,
    private val moonriseLabel: String,
    private val moonsetLabel: String,
    private val moonIlluminationLabel: String
) {
    private var legendPanel: LinearLayout? = null
    private var sunriseTimeView: TextView? = null
    private var sunsetTimeView: TextView? = null
    private var moonriseTimeView: TextView? = null
    private var moonsetTimeView: TextView? = null

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        // Find the legend panel from the parent view
        val rootView = explanationTextView.rootView
        legendPanel = rootView.findViewById(R.id.legendPanel)
        sunriseTimeView = rootView.findViewById(R.id.sunriseTime)
        sunsetTimeView = rootView.findViewById(R.id.sunsetTime)
        moonriseTimeView = rootView.findViewById(R.id.moonriseTime)
        moonsetTimeView = rootView.findViewById(R.id.moonsetTime)
    }

    fun showData(
        sunrise: ChronoFunction<CalendarDate, Moment>,
        sunset: ChronoFunction<CalendarDate, Moment>,
        moonrise: Moment?,
        moonset: Moment?
    ) {
        // Show the legend panel
        legendPanel?.visibility = View.VISIBLE

        // Get current date for calculations
        val today = PlainDate.nowInSystemTime()

        // Format and display times
        val sunriseTime = sunrise.apply(today)
        val sunsetTime = sunset.apply(today)

        sunriseTimeView?.text = formatTime(sunriseTime)
        sunsetTimeView?.text = formatTime(sunsetTime)
        moonriseTimeView?.text = moonrise?.let { formatTime(it) } ?: "--:--"
        moonsetTimeView?.text = moonset?.let { formatTime(it) } ?: "--:--"

        // Update the main explanation text
        updateExplanationText(sunriseTime, sunsetTime, moonrise, moonset)
    }

    private fun formatTime(moment: Moment): String {
        return try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = moment.posixTime * 1000
            timeFormat.format(calendar.time)
        } catch (e: Exception) {
            "--:--"
        }
    }

    private fun updateExplanationText(
        sunrise: Moment,
        sunset: Moment,
        moonrise: Moment?,
        moonset: Moment?
    ) {
        val sunriseStr = formatTime(sunrise)
        val sunsetStr = formatTime(sunset)
        val moonriseStr = moonrise?.let { formatTime(it) } ?: "N/A"
        val moonsetStr = moonset?.let { formatTime(it) } ?: "N/A"

        val explanationText = "Tap on the map to see sun and moon directions. " +
                "$sunriseLabel: $sunriseStr, $sunsetLabel: $sunsetStr, " +
                "$moonriseLabel: $moonriseStr, $moonsetLabel: $moonsetStr"

        explanationTextView.text = explanationText
    }

    fun hideLegend() {
        legendPanel?.visibility = View.GONE
    }
}
