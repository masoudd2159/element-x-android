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

internal object IranSansXFont {
    val family = FontFamily(
        Font(R.font.iran_sans_x_regular, FontWeight.W400),
        Font(R.font.iran_sans_x_bold, FontWeight.W700),
    )
}
