/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import kotlinx.collections.immutable.toImmutableList

@Inject
class ReadReceiptBottomSheetPresenter(private val dateFormatter: DateFormatter) : Presenter<ReadReceiptBottomSheetState> {
    @Composable
    override fun present(): ReadReceiptBottomSheetState {
        var selectedEvent: TimelineItem.Event? by remember { mutableStateOf(null) }

        fun handleEvent(event: ReadReceiptBottomSheetEvent) {
            @Suppress("LiftReturnOrAssignment")
            when (event) {
                is ReadReceiptBottomSheetEvent.EventSelected -> {
                    selectedEvent = event.event
                }
                ReadReceiptBottomSheetEvent.Dismiss -> {
                    selectedEvent = null
                }
            }
        }

        val dateRevision by dateFormatter.revisions.collectAsState()
        val eventWithDates = remember(selectedEvent, dateRevision) {
            selectedEvent?.let { event ->
                val receipts = event.readReceiptState.receipts
                val updatedReceipts = receipts.map { receipt ->
                    val timestamp = receipt.timestamp ?: return@map receipt
                    receipt.copy(formattedDate = dateFormatter.format(timestamp, DateFormatterMode.TimeOrDate))
                }
                if (updatedReceipts == receipts) {
                    event
                } else {
                    event.copy(readReceiptState = event.readReceiptState.copy(receipts = updatedReceipts.toImmutableList()))
                }
            }
        }

        return ReadReceiptBottomSheetState(
            selectedEvent = eventWithDates,
            eventSink = ::handleEvent,
        )
    }
}
