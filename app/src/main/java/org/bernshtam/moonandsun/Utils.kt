package org.bernshtam.moonandsun

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


val TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(java.time.temporal.ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(java.time.temporal.ChronoField.MINUTE_OF_HOUR, 2)
    .toFormatter()
