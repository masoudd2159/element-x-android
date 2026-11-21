/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreCalendarPreferencesTest {
    @Test
    fun `recovers the saved choice after presenting a fallback for read errors`() = runTest {
        val store = CalendarDataStore().apply { failReads = true }
        val preferences = DataStoreCalendarPreferences(store, backgroundScope)

        runCurrent()
        advanceTimeBy(1.seconds)
        runCurrent()
        assertThat(preferences.calendarType.value).isNull()

        advanceTimeBy(1.seconds)
        runCurrent()
        assertThat(preferences.calendarType.value).isEqualTo(CalendarType.PERSIAN)

        store.failReads = false
        advanceTimeBy(30.seconds)
        runCurrent()
        assertThat(preferences.calendarType.value).isEqualTo(CalendarType.GREGORIAN)
    }

    @Test
    fun `stops retrying persistent read errors and reconnects after a successful save`() = runTest {
        val store = CalendarDataStore().apply {
            failReads = true
            failWrites = true
        }
        val preferences = DataStoreCalendarPreferences(store, backgroundScope)

        runCurrent()
        advanceTimeBy(60.seconds)
        runCurrent()
        assertThat(store.readAttempts).isEqualTo(4)
        assertThat(preferences.calendarType.value).isEqualTo(CalendarType.PERSIAN)

        preferences.selectCalendar(CalendarType.HIJRI)
        runCurrent()
        assertThat(preferences.calendarType.value).isEqualTo(CalendarType.PERSIAN)
        assertThat(store.readAttempts).isEqualTo(4)

        store.failReads = false
        store.failWrites = false
        preferences.selectCalendar(CalendarType.HIJRI)
        runCurrent()
        assertThat(preferences.calendarType.value).isEqualTo(CalendarType.HIJRI)
    }

    private class CalendarDataStore : DataStore<Preferences>, PreferenceDataStoreFactory {
        var failReads = false
        var failWrites = false
        var readAttempts = 0
            private set

        private val savedPreferences = MutableStateFlow(preferencesOf(stringPreferencesKey("calendar_type") to CalendarType.GREGORIAN.name))

        override val data = flow {
            readAttempts++
            if (failReads) throw IOException("Read failed")
            emitAll(savedPreferences)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("Write failed")
            return transform(savedPreferences.value).also { savedPreferences.value = it }
        }

        override fun create(name: String): DataStore<Preferences> = this
    }
}
