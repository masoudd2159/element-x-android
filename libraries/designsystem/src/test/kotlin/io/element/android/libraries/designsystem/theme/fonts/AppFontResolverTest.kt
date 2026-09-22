/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme.fonts

import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AppFont
import org.junit.Test

class AppFontResolverTest {
    @Test
    fun `Yekan Bakh resolves to the Yekan Bakh family`() {
        assertThat(resolveAppFont(AppFont.YEKAN_BAKH)).isSameInstanceAs(YekanBakhFont.family)
    }

    @Test
    fun `IRANSansX resolves to the IRANSansX family`() {
        assertThat(resolveAppFont(AppFont.IRAN_SANS_X)).isSameInstanceAs(IranSansXFont.family)
    }
}
