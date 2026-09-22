/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.tokens

import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.tokens.generated.TypographyTokens
import org.junit.Test

class ElementTypographyTest {
    @Test
    fun `runtime typography preserves generated token properties and replaces only font family`() {
        val typography = elementTypography(FontFamily.Serif)

        listOf(
            typography.fontBodyLgMedium to TypographyTokens.fontBodyLgMedium,
            typography.fontBodyLgRegular to TypographyTokens.fontBodyLgRegular,
            typography.fontBodyMdMedium to TypographyTokens.fontBodyMdMedium,
            typography.fontBodyMdRegular to TypographyTokens.fontBodyMdRegular,
            typography.fontBodySmMedium to TypographyTokens.fontBodySmMedium,
            typography.fontBodySmRegular to TypographyTokens.fontBodySmRegular,
            typography.fontBodyXsMedium to TypographyTokens.fontBodyXsMedium,
            typography.fontBodyXsRegular to TypographyTokens.fontBodyXsRegular,
            typography.fontHeadingLgBold to TypographyTokens.fontHeadingLgBold,
            typography.fontHeadingLgRegular to TypographyTokens.fontHeadingLgRegular,
            typography.fontHeadingMdBold to TypographyTokens.fontHeadingMdBold,
            typography.fontHeadingMdRegular to TypographyTokens.fontHeadingMdRegular,
            typography.fontHeadingSmMedium to TypographyTokens.fontHeadingSmMedium,
            typography.fontHeadingSmRegular to TypographyTokens.fontHeadingSmRegular,
            typography.fontHeadingXlBold to TypographyTokens.fontHeadingXlBold,
            typography.fontHeadingXlRegular to TypographyTokens.fontHeadingXlRegular,
        ).forEach { (resolved, generated) ->
            assertThat(resolved.fontFamily).isEqualTo(FontFamily.Serif)
            assertThat(resolved.fontSize).isEqualTo(generated.fontSize)
            assertThat(resolved.fontWeight).isEqualTo(generated.fontWeight)
            assertThat(resolved.lineHeight).isEqualTo(generated.lineHeight)
            assertThat(resolved.letterSpacing).isEqualTo(generated.letterSpacing)
            assertThat(resolved.platformStyle).isEqualTo(generated.platformStyle)
            assertThat(resolved.lineHeightStyle).isEqualTo(generated.lineHeightStyle)
        }
    }

    @Test
    fun `material typography uses the same font family as element typography`() {
        val fontFamily = FontFamily.Serif
        val elementTypography = elementTypography(fontFamily)
        val materialTypography = compoundTypography(fontFamily)

        assertThat(elementTypography.fontBodyLgRegular.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.bodyLarge.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.titleMedium.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.headlineLarge.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.labelMedium.fontFamily).isEqualTo(fontFamily)
    }
}
