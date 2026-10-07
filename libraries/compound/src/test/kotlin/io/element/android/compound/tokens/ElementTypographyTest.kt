/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AppFont
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.fonts.resolveAppFont
import io.element.android.compound.tokens.generated.TypographyTokens
import org.junit.Test

// A top-level initializer deliberately exercises upstream's non-Composable typography contract.
private val topLevelBodyStyle = ElementTheme.typography.fontBodySmRegular

class ElementTypographyTest {
    @Test
    fun `static theme typography uses the configured application font`() {
        assertThat(ElementTheme.typography).isSameInstanceAs(PoopakTypography.element)
        assertThat(topLevelBodyStyle.fontFamily).isSameInstanceAs(resolveAppFont())
        assertThat(PoopakTypography.material.bodyLarge.fontFamily).isSameInstanceAs(resolveAppFont())
    }

    @Test
    fun `both app fonts cover every generated token without changing other properties`() {
        val tokenGetters = TypographyTokens::class.java.methods.filter { it.returnType == TextStyle::class.java }
        val facadeGetters = ElementTypography::class.java.methods.filter { it.returnType == TextStyle::class.java }
        assertThat(facadeGetters.map { it.name }).containsExactlyElementsIn(tokenGetters.map { it.name })
        AppFont.entries.forEach { appFont ->
            val family = resolveAppFont(appFont)
            val typography = elementTypography(family)
            tokenGetters.forEach { getter ->
                val token = getter.invoke(TypographyTokens) as TextStyle
                val style = ElementTypography::class.java.getMethod(getter.name).invoke(typography) as TextStyle
                assertThat(style).isEqualTo(token.copy(fontFamily = family))
            }
            val material = compoundTypography.withFontFamily(family)
            compoundTypography::class.java.methods.filter { it.returnType == TextStyle::class.java }.forEach { getter ->
                val token = getter.invoke(compoundTypography) as TextStyle
                val style = getter.invoke(material) as TextStyle
                assertThat(style).isEqualTo(token.copy(fontFamily = family))
            }
        }
    }

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
            assertThat(resolved).isEqualTo(generated.copy(fontFamily = FontFamily.Serif))
        }
    }

    @Test
    fun `material typography uses the same font family as element typography`() {
        val fontFamily = FontFamily.Serif
        val elementTypography = elementTypography(fontFamily)
        val materialTypography = compoundTypography.withFontFamily(fontFamily)

        assertThat(elementTypography.fontBodyLgRegular.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.bodyLarge.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.titleMedium.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.headlineLarge.fontFamily).isEqualTo(fontFamily)
        assertThat(materialTypography.labelMedium.fontFamily).isEqualTo(fontFamily)
    }
}
