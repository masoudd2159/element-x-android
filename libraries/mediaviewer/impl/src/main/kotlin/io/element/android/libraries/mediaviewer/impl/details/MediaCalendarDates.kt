/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.mediaviewer.api.MediaInfo

internal fun MediaInfo.withCalendarDates(formatter: DateFormatter): MediaInfo {
    val timestamp = sentTimestamp ?: return this
    return copy(
        dateSent = formatter.format(timestamp, DateFormatterMode.Day),
        dateSentFull = formatter.format(timestamp, DateFormatterMode.Full),
    )
}

@Composable
internal fun MediaBottomSheetState.withCalendarDates(formatter: DateFormatter, revision: Long): MediaBottomSheetState =
    remember(this, revision) {
        when (this) {
            is MediaBottomSheetState.Details -> copy(mediaInfo = mediaInfo.withCalendarDates(formatter))
            else -> this
        }
    }
