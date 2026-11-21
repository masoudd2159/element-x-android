/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.dateformatter.api.CalendarPreferences
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

private const val READ_RETRIES_BEFORE_FALLBACK = 2L

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreCalendarPreferences(
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
    @AppCoroutineScope appScope: CoroutineScope,
) : CalendarPreferences {
    private val store = preferenceDataStoreFactory.create("calendar_preferences")
    private val key = stringPreferencesKey("calendar_type")
    private val selections = Channel<CalendarType>(Channel.CONFLATED)
    private val readRequests = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val calendarType = readRequests
        .flatMapLatest {
            store.data
                .map { preferences ->
                    CalendarType.entries.firstOrNull { it.name == preferences[key] } ?: CalendarType.PERSIAN
                }
                .retryWhen { error, attempt ->
                    if (error !is IOException || attempt > READ_RETRIES_BEFORE_FALLBACK) return@retryWhen false
                    if (attempt == READ_RETRIES_BEFORE_FALLBACK) {
                        Timber.w("Using the default calendar while storage is unavailable")
                        emit(CalendarType.PERSIAN)
                        // Allow one later recovery attempt without keeping dates hidden or polling indefinitely.
                        delay(30.seconds)
                    } else {
                        delay(1.seconds)
                    }
                    true
                }
                .catch { error ->
                    if (error !is IOException) throw error
                    Timber.w("Could not read the calendar preference after retrying")
                    emit(CalendarType.PERSIAN)
                }
        }
        .buffer(0)
        .stateIn(appScope, SharingStarted.Eagerly, null)

    init {
        appScope.launch {
            for (type in selections) {
                try {
                    store.edit { preferences -> preferences[key] = type.name }
                    // Reconnect the reader if its recovery attempts were exhausted.
                    readRequests.update { it + 1 }
                } catch (_: IOException) {
                    Timber.w("Could not save the calendar preference")
                }
            }
        }
    }

    override fun selectCalendar(type: CalendarType) {
        selections.trySend(type)
    }
}
