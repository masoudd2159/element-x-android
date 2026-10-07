/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import io.element.android.compound.theme.fonts.resolveAppFont
import io.element.android.compound.tokens.generated.TypographyTokens

/**
 * Immutable application-wide typography for the font selected in CustomAppConfig.
 *
 * Initialization is independent of composition, so top-level styles and nested themes use the
 * same font. Font selection is fixed for the process; changing configuration requires a rebuild.
 */
internal object PoopakTypography {
    private val fontFamily = resolveAppFont()
    val element = elementTypography(fontFamily)
    val material = compoundTypography.withFontFamily(fontFamily)
}

/**
 * Application-wide Compound typography derived from generated [TypographyTokens].
 *
 * Keep this facade in sync with [TypographyTokens] when upstream Compound adds, removes, or renames typography tokens.
 * Token dimensions and styling must remain sourced from [TypographyTokens]; this facade exists only to apply the configured font family.
 */
class ElementTypography internal constructor(
    val fontBodyLgMedium: TextStyle,
    val fontBodyLgRegular: TextStyle,
    val fontBodyMdMedium: TextStyle,
    val fontBodyMdRegular: TextStyle,
    val fontBodySmMedium: TextStyle,
    val fontBodySmRegular: TextStyle,
    val fontBodyXsMedium: TextStyle,
    val fontBodyXsRegular: TextStyle,
    val fontHeadingLgBold: TextStyle,
    val fontHeadingLgRegular: TextStyle,
    val fontHeadingMdBold: TextStyle,
    val fontHeadingMdRegular: TextStyle,
    val fontHeadingSmMedium: TextStyle,
    val fontHeadingSmRegular: TextStyle,
    val fontHeadingXlBold: TextStyle,
    val fontHeadingXlRegular: TextStyle,
)

/**
 * Applies [fontFamily] to generated Compound typography without duplicating its token values.
 */
internal fun elementTypography(fontFamily: FontFamily = FontFamily.Default) = ElementTypography(
    fontBodyLgMedium = TypographyTokens.fontBodyLgMedium.copy(fontFamily = fontFamily),
    fontBodyLgRegular = TypographyTokens.fontBodyLgRegular.copy(fontFamily = fontFamily),
    fontBodyMdMedium = TypographyTokens.fontBodyMdMedium.copy(fontFamily = fontFamily),
    fontBodyMdRegular = TypographyTokens.fontBodyMdRegular.copy(fontFamily = fontFamily),
    fontBodySmMedium = TypographyTokens.fontBodySmMedium.copy(fontFamily = fontFamily),
    fontBodySmRegular = TypographyTokens.fontBodySmRegular.copy(fontFamily = fontFamily),
    fontBodyXsMedium = TypographyTokens.fontBodyXsMedium.copy(fontFamily = fontFamily),
    fontBodyXsRegular = TypographyTokens.fontBodyXsRegular.copy(fontFamily = fontFamily),
    fontHeadingLgBold = TypographyTokens.fontHeadingLgBold.copy(fontFamily = fontFamily),
    fontHeadingLgRegular = TypographyTokens.fontHeadingLgRegular.copy(fontFamily = fontFamily),
    fontHeadingMdBold = TypographyTokens.fontHeadingMdBold.copy(fontFamily = fontFamily),
    fontHeadingMdRegular = TypographyTokens.fontHeadingMdRegular.copy(fontFamily = fontFamily),
    fontHeadingSmMedium = TypographyTokens.fontHeadingSmMedium.copy(fontFamily = fontFamily),
    fontHeadingSmRegular = TypographyTokens.fontHeadingSmRegular.copy(fontFamily = fontFamily),
    fontHeadingXlBold = TypographyTokens.fontHeadingXlBold.copy(fontFamily = fontFamily),
    fontHeadingXlRegular = TypographyTokens.fontHeadingXlRegular.copy(fontFamily = fontFamily),
)

internal fun Typography.withFontFamily(fontFamily: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
)
