/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.datasource

import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.matrix.api.core.UniqueId
import io.element.android.libraries.mediaviewer.impl.model.GroupedMediaItems
import io.element.android.libraries.mediaviewer.impl.model.MediaItem
import io.element.android.libraries.mediaviewer.impl.model.id
import io.element.android.libraries.mediaviewer.impl.model.mediaInfo
import kotlinx.collections.immutable.toImmutableList

internal fun DateFormatter.groupMediaByMonth(items: List<MediaItem>): GroupedMediaItems {
    val images = MonthSections(this)
    val files = MonthSections(this)
    for (item in items) {
        when (item) {
            is MediaItem.Image, is MediaItem.Video -> images.add(item)
            is MediaItem.Event -> files.add(item)
            is MediaItem.LoadingIndicator -> {
                images.items.add(item)
                files.items.add(item)
            }
            // SDK sections follow Gregorian month boundaries and their cached labels may be stale.
            is MediaItem.DateSeparator -> Unit
        }
    }
    return GroupedMediaItems(images.items.toImmutableList(), files.items.toImmutableList())
}

private class MonthSections(private val formatter: DateFormatter) {
    val items = mutableListOf<MediaItem>()
    private var lastMonth: Long? = null

    fun add(item: MediaItem.Event) {
        val timestamp = item.mediaInfo().sentTimestamp
        val month = timestamp?.let(formatter::monthKey)
        if (month != null && month != lastMonth) {
            val date = formatter.format(timestamp, DateFormatterMode.Month, useRelative = true)
            if (date.isNotBlank()) {
                items.add(MediaItem.DateSeparator(id = UniqueId("calendar-month-${item.id().value}"), formattedDate = date))
                lastMonth = month
            }
        }
        items.add(item)
    }
}
