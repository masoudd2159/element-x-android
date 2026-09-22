/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme.fonts

import androidx.compose.ui.text.font.FontFamily
import io.element.android.appconfig.AppFont
import io.element.android.appconfig.CustomAppConfig

internal fun resolveAppFont(appFont: AppFont = CustomAppConfig.Appearance.FONT): FontFamily = when (appFont) {
    AppFont.YEKAN_BAKH -> YekanBakhFont.family
    AppFont.IRAN_SANS_X -> IranSansXFont.family
}
