/*
 * Copyright (c) 2026 MyPoopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package ir.mypoopak.android.brandconfig

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class BrandFirebaseGatewayProviderTest {
    @Test
    fun `returns the configured brand Firebase push gateway`() {
        val expectedGateway = "https://api.mypoopak.ir/_matrix/push/v1/notify"
        val context = mockk<Context> {
            every { getString(R.string.brand_firebase_push_gateway) } returns expectedGateway
        }
        val provider = BrandFirebaseGatewayProvider(context)

        assertThat(provider.getFirebaseGateway()).isEqualTo(expectedGateway)
    }
}
