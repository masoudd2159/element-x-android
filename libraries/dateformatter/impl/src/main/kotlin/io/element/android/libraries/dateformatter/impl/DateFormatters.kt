/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateFormat
import android.text.format.DateUtils
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import java.time.Period
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant

@SingleIn(AppScope::class)
@Inject
class DateFormatters(
    localeChangeObserver: LocaleChangeObserver,
    private val clock: Clock,
    private val timeZoneProvider: TimezoneProvider,
    locale: Locale,
    @ApplicationContext private val context: Context? = null,
) : LocaleChangeListener {
    init {
        localeChangeObserver.addListener(this)
    }

    @Volatile
    private var dateTimeFormatters: DateTimeFormatters = DateTimeFormatters(applicationLocale() ?: locale)

    override fun onLocaleChange() {
        Timber.w("Locale changed, updating formatters")
        dateTimeFormatters = DateTimeFormatters(applicationLocale() ?: Locale.getDefault())
    }

    internal fun applicationLocale(): Locale? = context?.resources?.configuration?.locales?.get(0)

    internal fun formatTime(localDateTime: LocalDateTime): String {
        val formatters = dateTimeFormatters
        val formatter = when (context?.let(DateFormat::is24HourFormat)) {
            true -> formatters.time24HourFormatter
            false -> formatters.time12HourFormatter
            null -> formatters.onlyTimeFormatter
        }
        return formatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithMonthAndYear(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithMonthAndYearFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithMonth(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithMonthFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithDay(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithDayFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithYear(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithYearFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithFullFormat(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithFullFormatFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDateWithFullFormatNoYear(localDateTime: LocalDateTime): String {
        return dateTimeFormatters.dateWithFullFormatNoYearFormatter.format(localDateTime.toJavaLocalDateTime())
    }

    internal fun formatDate(
        dateToFormat: LocalDateTime,
        currentDate: LocalDateTime,
        useRelative: Boolean
    ): String {
        val period = Period.between(dateToFormat.date.toJavaLocalDate(), currentDate.date.toJavaLocalDate())
        return if (period.years.absoluteValue >= 1) {
            formatDateWithYear(dateToFormat)
        } else if (useRelative && period.days.absoluteValue < 2 && period.months.absoluteValue < 1) {
            getRelativeDay(dateToFormat.toInstant(timeZoneProvider.provide()).toEpochMilliseconds())
        } else {
            formatDateWithMonth(dateToFormat)
        }
    }

    internal fun getRelativeDay(ts: Long, default: String = ""): String {
        val appLocale = applicationLocale()
        if (appLocale != null && appLocale != Locale.getDefault()) {
            val zone = timeZoneProvider.provide()
            val today = clock.now().toLocalDateTime(zone).date
            val day = Instant.fromEpochMilliseconds(ts).toLocalDateTime(zone).date
            val direction = when (today.toEpochDays() - day.toEpochDays()) {
                0L -> RelativeDateTimeFormatter.Direction.THIS
                1L -> RelativeDateTimeFormatter.Direction.LAST
                -1L -> RelativeDateTimeFormatter.Direction.NEXT
                else -> null
            }
            if (direction != null) {
                return RelativeDateTimeFormatter.getInstance(appLocale).format(direction, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
            }
        }
        return DateUtils.getRelativeTimeSpanString(
            ts,
            clock.now().toEpochMilliseconds(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_SHOW_WEEKDAY
        )?.toString() ?: default
    }
}
