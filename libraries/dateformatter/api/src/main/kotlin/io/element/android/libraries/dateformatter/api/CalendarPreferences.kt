/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.api

import kotlinx.coroutines.flow.StateFlow

enum class CalendarType {
    PERSIAN,
    GREGORIAN,
    HIJRI,
}

interface CalendarPreferences {
    /** Null until the saved application-wide preference has been read. */
    val calendarType: StateFlow<CalendarType?>

    fun selectCalendar(type: CalendarType)
}
