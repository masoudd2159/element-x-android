/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.androidutils.system.DateTimeObserver
import io.element.android.libraries.dateformatter.api.CalendarPreferences
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CalendarDateFormatter(
    private val gregorian: DefaultDateFormatter,
    private val engine: CalendarDateEngine,
    private val preferences: CalendarPreferences,
    dateTimeObserver: DateTimeObserver,
    localeChangeObserver: LocaleChangeObserver,
    @ApplicationContext context: Context,
    @AppCoroutineScope appScope: CoroutineScope,
) : DateFormatter {
    private val revision = MutableStateFlow(0L)
    override val revisions = revision.asStateFlow()

    init {
        preferences.calendarType.filterNotNull().onEach { invalidateDates() }.launchIn(appScope)
        dateTimeObserver.changes.onEach { invalidateDates() }.launchIn(appScope)
        localeChangeObserver.addListener(object : LocaleChangeListener {
            override fun onLocaleChange() = invalidateDates()
        })
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24),
            false,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = invalidateDates()
            },
        )
    }

    override fun format(timestamp: Long?, mode: DateFormatterMode, useRelative: Boolean): String {
        timestamp ?: return ""
        if (mode == DateFormatterMode.TimeOnly) return gregorian.format(timestamp, mode, useRelative)
        return when (val type = preferences.calendarType.value) {
            null -> ""
            CalendarType.GREGORIAN -> gregorian.format(timestamp, mode, useRelative)
            else -> engine.format(timestamp, type, mode, useRelative)
        }
    }

    override fun monthKey(timestamp: Long): Long? = preferences.calendarType.value?.let { engine.monthKey(timestamp, it) }

    private fun invalidateDates() = revision.update { it + 1 }
}
