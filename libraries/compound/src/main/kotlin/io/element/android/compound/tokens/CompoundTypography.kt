/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.element.android.compound.tokens.generated.TypographyTokens

/**
 * Runtime Compound typography derived from generated [TypographyTokens].
 *
 * Keep this facade in sync with [TypographyTokens] when upstream Compound adds, removes, or renames typography tokens.
 * Token dimensions and styling must remain sourced from [TypographyTokens]; this facade exists only to apply the runtime font family.
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

// 32px (Material) vs 34px, it's the closest one
internal val compoundHeadingXlRegular = TypographyTokens.fontHeadingXlRegular

// both are 28px
internal val compoundHeadingLgRegular = TypographyTokens.fontHeadingLgRegular

// These are the default M3 values, but we're setting them manually so an update in M3 doesn't break our designs
internal val defaultHeadlineSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    lineHeight = 32.sp,
    fontSize = 24.sp,
    letterSpacing = 0.em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
)

// 22px (Material) vs 20px, it's the closest one
internal val compoundHeadingMdRegular = TypographyTokens.fontHeadingMdRegular

// 16px both
internal val compoundBodyLgMedium = TypographyTokens.fontBodyLgMedium

// 14px both
internal val compoundBodyMdMedium = TypographyTokens.fontBodyMdMedium

// 16px both
internal val compoundBodyLgRegular = TypographyTokens.fontBodyLgRegular

// 14px both
internal val compoundBodyMdRegular = TypographyTokens.fontBodyMdRegular

// 12px both
internal val compoundBodySmRegular = TypographyTokens.fontBodySmRegular

// 12px both
internal val compoundBodySmMedium = TypographyTokens.fontBodySmMedium

// 11px both
internal val compoundBodyXsMedium = TypographyTokens.fontBodyXsMedium

internal fun compoundTypography(fontFamily: FontFamily = FontFamily.Default) = Typography(
    // displayLarge = , 57px (Material) size. We have no equivalent
    // displayMedium = , 45px (Material) size. We have no equivalent
    // displaySmall = , 36px (Material) size. We have no equivalent
    headlineLarge = compoundHeadingXlRegular.copy(fontFamily = fontFamily),
    headlineMedium = compoundHeadingLgRegular.copy(fontFamily = fontFamily),
    headlineSmall = defaultHeadlineSmall.copy(fontFamily = fontFamily),
    titleLarge = compoundHeadingMdRegular.copy(fontFamily = fontFamily),
    titleMedium = compoundBodyLgMedium.copy(fontFamily = fontFamily),
    titleSmall = compoundBodyMdMedium.copy(fontFamily = fontFamily),
    bodyLarge = compoundBodyLgRegular.copy(fontFamily = fontFamily),
    bodyMedium = compoundBodyMdRegular.copy(fontFamily = fontFamily),
    bodySmall = compoundBodySmRegular.copy(fontFamily = fontFamily),
    labelLarge = compoundBodyMdMedium.copy(fontFamily = fontFamily),
    labelMedium = compoundBodySmMedium.copy(fontFamily = fontFamily),
    labelSmall = compoundBodyXsMedium.copy(fontFamily = fontFamily),
)

internal val compoundTypography = compoundTypography()
