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
import io.element.android.appconfig.AppFont
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.preferences.DropdownOption
import io.element.android.libraries.designsystem.components.preferences.PreferenceDropdown
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun FontPreferenceDropdown(selected: AppFont, onSelect: (AppFont) -> Unit) {
    PreferenceDropdown(
        title = stringResource(R.string.screen_settings_app_font),
        selectedOption = options.first { it.font == selected },
        options = options,
        iconResourceId = R.drawable.ic_app_font,
        dropDownIcon = CompoundIcons.ChevronUpDown(),
        onSelectOption = { onSelect(it.font) },
    )
}

private data class FontOption(val font: AppFont) : DropdownOption {
    @Composable
    override fun getText(): String = stringResource(
        when (font) {
            AppFont.YEKAN_BAKH -> R.string.screen_settings_font_yekan_bakh
            AppFont.IRAN_SANS_X -> R.string.screen_settings_font_iran_sans_x
        }
    )
}

private val options = AppFont.entries.map(::FontOption).toImmutableList()
