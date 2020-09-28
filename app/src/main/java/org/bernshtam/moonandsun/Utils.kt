package org.bernshtam.moonandsun

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


val TIME: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(java.time.temporal.ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(java.time.temporal.ChronoField.MINUTE_OF_HOUR, 2)
    .toFormatter()


val DATE: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendLiteral(' ')
    .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR)
    .appendLiteral(' ')
    .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral(", ")
    .appendValue(java.time.temporal.ChronoField.YEAR, 4)
    .toFormatter()
