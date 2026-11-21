/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.dateformatter.api.CalendarType
import io.element.android.libraries.designsystem.components.preferences.DropdownOption
import io.element.android.libraries.designsystem.components.preferences.PreferenceDropdown
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun CalendarPreferenceDropdown(selected: CalendarType?, onSelect: (CalendarType) -> Unit) {
    PreferenceDropdown(
        title = stringResource(R.string.screen_settings_calendar_type),
        selectedOption = options.firstOrNull { it.type == selected },
        options = options,
        enabled = selected != null,
        icon = CompoundIcons.Calendar(),
        dropDownIcon = CompoundIcons.ChevronUpDown(),
        onSelectOption = { onSelect(it.type) },
    )
}

private data class CalendarOption(val type: CalendarType) : DropdownOption {
    @Composable
    override fun getText(): String = stringResource(
        when (type) {
            CalendarType.PERSIAN -> R.string.screen_settings_calendar_persian
            CalendarType.GREGORIAN -> R.string.screen_settings_calendar_gregorian
            CalendarType.HIJRI -> R.string.screen_settings_calendar_hijri
        }
    )
}

private val options = CalendarType.entries.map(::CalendarOption).toImmutableList()
