/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AppFont
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.compound.theme.fonts.resolveAppFont
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ElementThemeTypographyTest : RobolectricTest() {
    @Test
    fun `rendered text updates in nested themes on the same Activity`() = runAndroidComposeUiTest<ComponentActivity> {
        val appFont = mutableStateOf(AppFont.YEKAN_BAKH)
        var compoundFamily: FontFamily? = null
        var materialFamily: FontFamily? = null
        var overrideFamily: FontFamily? = null

        setContent {
            ElementTheme(theme = Theme.Light, appFont = appFont.value, applySystemBarsUpdate = false) {
                ForcedDarkElementTheme(colors = SemanticColorsLightDark.default) {
                    Text(
                        text = "سلام",
                        style = ElementTheme.typography.fontBodyLgRegular,
                        onTextLayout = { compoundFamily = it.layoutInput.style.fontFamily },
                    )
                    Text(
                        text = "سلام Hello",
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = { materialFamily = it.layoutInput.style.fontFamily },
                    )
                    ElementTheme(theme = Theme.Light, appFont = AppFont.YEKAN_BAKH, applySystemBarsUpdate = false) {
                        Text(
                            text = "Hello",
                            onTextLayout = { overrideFamily = it.layoutInput.style.fontFamily },
                        )
                    }
                }
            }
        }

        val originalActivity = activity
        runOnIdle {
            assertThat(compoundFamily).isEqualTo(resolveAppFont(AppFont.YEKAN_BAKH))
            assertThat(materialFamily).isEqualTo(compoundFamily)
            assertThat(overrideFamily).isEqualTo(compoundFamily)
            appFont.value = AppFont.IRAN_SANS_X
        }

        runOnIdle {
            assertThat(activity).isSameInstanceAs(originalActivity)
            assertThat(compoundFamily).isEqualTo(resolveAppFont(AppFont.IRAN_SANS_X))
            assertThat(materialFamily).isEqualTo(compoundFamily)
            assertThat(overrideFamily).isEqualTo(resolveAppFont(AppFont.YEKAN_BAKH))
        }
    }
}
