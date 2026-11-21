/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import android.icu.text.DateFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.extensions.safeCapitalize
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.services.toolbox.api.strings.StringProvider
import java.util.Date
import java.util.Locale
import kotlin.time.Clock

@Inject
class CalendarDateEngine(
    private val clock: Clock,
    private val timeZoneProvider: TimezoneProvider,
    private val localDateTimeProvider: LocalDateTimeProvider,
    private val dateFormatters: DateFormatters,
    private val strings: StringProvider,
) {
    // ICU calendars and formatters are mutable. Reuse them only on their owning thread.
    private val formatsByThread = ThreadLocal<Formats>()

    fun monthKey(timestamp: Long, type: CalendarType): Long {
        return formats(type).fields(timestamp).monthKey
    }

    fun format(timestamp: Long, type: CalendarType, mode: DateFormatterMode, useRelative: Boolean = false): String {
        val formats = formats(type)
        val day = formats.fields(timestamp)
        val today = formats.fields(clock.now().toEpochMilliseconds())
        val daysAgo = today.julianDay - day.julianDay

        fun time() = dateFormatters.formatTime(localDateTimeProvider.providesFromTimestamp(timestamp))
        fun relativeDay() = formats.relativeDay(daysAgo)
        fun dayLabel(): String = when {
            useRelative && daysAgo in 0..1 -> relativeDay()
            useRelative && daysAgo in 2..6 && day.year == today.year -> formats.date("EEEE", timestamp)
            day.year == today.year -> formats.date("EEEEdMMMM", timestamp)
            else -> formats.date("yMMMMd", timestamp)
        }.safeCapitalize()

        return when (mode) {
            DateFormatterMode.Day -> dayLabel()
            DateFormatterMode.Month -> if (useRelative && day.monthKey == today.monthKey) {
                strings.getString(R.string.common_date_this_month)
            } else {
                formats.date("yMMMM", timestamp)
            }.safeCapitalize()
            DateFormatterMode.Full -> if (useRelative && daysAgo == 0) {
                time()
            } else {
                val date = if (useRelative) dayLabel() else formats.date("yMMMMd", timestamp)
                strings.getString(R.string.common_date_date_at_time, date, time())
            }
            DateFormatterMode.TimeOrDate -> when {
                daysAgo == 0 -> time()
                useRelative && daysAgo == 1 -> relativeDay().safeCapitalize()
                day.year == today.year -> formats.date("MMMd", timestamp)
                else -> formats.date("yMd", timestamp)
            }
            DateFormatterMode.TimeOnly -> time()
        }
    }

    private fun formats(type: CalendarType): Formats {
        val locale = dateFormatters.applicationLocale() ?: Locale.getDefault()
        val timeZoneId = timeZoneProvider.provide().id
        val current = formatsByThread.get()
        if (current != null && current.type == type && current.locale == locale && current.timeZoneId == timeZoneId) return current
        return Formats(type, locale, timeZoneId).also(formatsByThread::set)
    }

    private data class Fields(val julianDay: Int, val year: Int, val month: Int) {
        val monthKey: Long get() = year.toLong() * 12 + month
    }

    private class Formats(val type: CalendarType, val locale: Locale, val timeZoneId: String) {
        private val calendarId = when (type) {
            CalendarType.PERSIAN -> "persian"
            CalendarType.GREGORIAN -> "gregorian"
            // Tabular civil calculation. Official moon-sighting dates can differ.
            CalendarType.HIJRI -> "islamic-civil"
        }
        private val calendarLocale = ULocale.forLocale(locale).setKeywordValue("calendar", calendarId)
        private val timeZone = TimeZone.getTimeZone(timeZoneId)
        private val calendar = Calendar.getInstance(timeZone, calendarLocale)
        private val dateFormats = mutableMapOf<String, DateFormat>()
        private val relatives by lazy { RelativeDateTimeFormatter.getInstance(calendarLocale) }

        fun fields(timestamp: Long): Fields {
            calendar.timeInMillis = timestamp
            return Fields(calendar[Calendar.JULIAN_DAY], calendar[Calendar.EXTENDED_YEAR], calendar[Calendar.MONTH])
        }

        fun date(skeleton: String, timestamp: Long): String {
            val format = dateFormats.getOrPut(skeleton) {
                DateFormat.getInstanceForSkeleton(skeleton, calendarLocale).apply { this.timeZone = this@Formats.timeZone }
            }
            return format.format(Date(timestamp))
        }

        fun relativeDay(daysAgo: Int): String = relatives.format(
            if (daysAgo == 0) RelativeDateTimeFormatter.Direction.THIS else RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY,
        )
    }
}
