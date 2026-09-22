/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme.fonts

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.element.android.libraries.designsystem.R

internal object YekanBakhFont {
    val family = FontFamily(
        Font(R.font.yekan_bakh_regular, FontWeight.W400),
        Font(R.font.yekan_bakh_medium, FontWeight.W500),
        Font(R.font.yekan_bakh_semi_bold, FontWeight.W600),
        Font(R.font.yekan_bakh_bold, FontWeight.W700),
    )
}
