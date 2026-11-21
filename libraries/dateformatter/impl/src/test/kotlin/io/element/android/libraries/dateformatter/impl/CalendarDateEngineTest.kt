/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.tests.testutils.InstrumentationStringProvider
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.time.Instant

@Config(qualifiers = "en", sdk = [Build.VERSION_CODES.TIRAMISU])
class CalendarDateEngineTest : RobolectricTest() {
    @Test
    fun `Nowruz and relative month follow Tehran local midnight`() {
        val engine = engine("2025-03-21T07:00:00Z", "Asia/Tehran")
        val lastDay = millis("2025-03-20T20:29:59Z")
        val nowruz = millis("2025-03-20T20:30:00Z")

        assertThat(engine.format(lastDay, CalendarType.PERSIAN, DateFormatterMode.Full)).contains("Esfand 30, 1403")
        assertThat(engine.format(nowruz, CalendarType.PERSIAN, DateFormatterMode.Full)).contains("Farvardin 1, 1404")
        assertThat(engine.format(lastDay, CalendarType.PERSIAN, DateFormatterMode.Day, useRelative = true)).isEqualTo("Yesterday")
        assertThat(engine.format(nowruz, CalendarType.PERSIAN, DateFormatterMode.Month, useRelative = true)).isEqualTo("This month")
        assertThat(engine.monthKey(lastDay, CalendarType.PERSIAN)).isNotEqualTo(engine.monthKey(nowruz, CalendarType.PERSIAN))
    }

    @Test
    fun `civil Hijri year boundary falls within one Gregorian month`() {
        val engine = engine("2025-06-27T12:00:00Z", "UTC")
        val oldYear = millis("2025-06-26T12:00:00Z")
        val newYear = millis("2025-06-27T12:00:00Z")

        assertThat(engine.format(oldYear, CalendarType.HIJRI, DateFormatterMode.Month, useRelative = true)).contains("1446")
        assertThat(engine.format(newYear, CalendarType.HIJRI, DateFormatterMode.Month, useRelative = true)).isEqualTo("This month")
        assertThat(engine.monthKey(oldYear, CalendarType.HIJRI)).isNotEqualTo(engine.monthKey(newYear, CalendarType.HIJRI))
        assertThat(engine.monthKey(oldYear, CalendarType.GREGORIAN)).isEqualTo(engine.monthKey(newYear, CalendarType.GREGORIAN))
    }

    private fun engine(now: String, zone: String): CalendarDateEngine {
        val clock = FakeClock().apply { givenInstant(Instant.parse(now)) }
        val timeZoneProvider = TimezoneProvider { TimeZone.of(zone) }
        return CalendarDateEngine(
            clock = clock,
            timeZoneProvider = timeZoneProvider,
            localDateTimeProvider = LocalDateTimeProvider(clock, timeZoneProvider),
            dateFormatters = DateFormatters({}, clock, timeZoneProvider, Locale.getDefault()),
            strings = InstrumentationStringProvider(),
        )
    }

    private fun millis(value: String) = Instant.parse(value).toEpochMilliseconds()
}
