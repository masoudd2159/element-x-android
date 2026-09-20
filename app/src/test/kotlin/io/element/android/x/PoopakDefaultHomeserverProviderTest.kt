/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PoopakDefaultHomeserverProviderTest {
    @Test
    fun `provider uses the generated backend host`() {
        assertThat(PoopakDefaultHomeserverProvider().getDefaultHomeserver()).isEqualTo("https://${BuildConfig.BACKEND_HOST}")
    }

    @Test
    fun `host without scheme uses HTTPS`() {
        assertThat(normalizeHomeserver("matrix.example.test")).isEqualTo("https://matrix.example.test")
    }

    @Test
    fun `existing HTTPS scheme is not duplicated`() {
        assertThat(normalizeHomeserver("https://matrix.example.test/")).isEqualTo("https://matrix.example.test")
    }

    @Test
    fun `existing HTTP scheme is not duplicated`() {
        assertThat(normalizeHomeserver("http://localhost:8008/")).isEqualTo("http://localhost:8008")
    }

    @Test
    fun `blank host returns null`() {
        assertThat(normalizeHomeserver("  ")).isNull()
    }
}
