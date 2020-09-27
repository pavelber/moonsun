package org.bernshtam.moonandsun

import net.time4j.Moment
import net.time4j.PlainDate
import net.time4j.engine.CalendarDate
import net.time4j.engine.ChronoFunction
import java.time.LocalDateTime
import java.util.*

val cal: Calendar = Calendar.getInstance()
val TZ: TimeZone = cal.timeZone



fun ChronoFunction<CalendarDate, Moment>.toLocalTimeDate(): LocalDateTime {
    val moment = PlainDate.nowInSystemTime().get(this)

    return moment.toLocalTimeDate()
}


fun Moment.toLocalTimeDate(): LocalDateTime {

    val zonal = this.toZonalTimestamp(TZ.id)
    return LocalDateTime.of(
        zonal.year,
        zonal.month,
        zonal.dayOfMonth,
        zonal.hour,
        zonal.minute,
        zonal.second,
        zonal.nanosecond
    )
}



