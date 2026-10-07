/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.qrcode

import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.DefaultHomeserverProvider
import io.element.android.libraries.matrix.test.auth.qrlogin.FakeMatrixQrCodeLoginData
import io.element.android.tests.testutils.lambda.lambdaError
import org.junit.Test

class QrLoginHomeserverValidatorTest {
    @Test
    fun `exact configured server is accepted`() {
        assertAllowed("https://api.mypoopak.ir")
    }

    @Test
    fun `QR host without scheme is accepted`() {
        assertAllowed("api.mypoopak.ir")
    }

    @Test
    fun `configured host without scheme is accepted`() {
        assertAllowed("https://api.mypoopak.ir", configured = "api.mypoopak.ir")
    }

    @Test
    fun `trailing slash is accepted on either server`() {
        assertAllowed("https://api.mypoopak.ir/")
        assertAllowed("api.mypoopak.ir", configured = "https://api.mypoopak.ir/")
    }

    @Test
    fun `hostname and scheme case differences are accepted`() {
        assertAllowed("HTTPS://API.MYPOOPAK.IR/")
        assertAllowed("api.mypoopak.ir", configured = "HTTPS://API.MYPOOPAK.IR/")
    }

    @Test
    fun `different homeserver is rejected`() {
        assertRejected("other.example.com")
    }

    @Test
    fun `subdomain is rejected`() {
        assertRejected("evil.api.mypoopak.ir")
    }

    @Test
    fun `suffix domain is rejected`() {
        assertRejected("api.mypoopak.ir.evil.com")
    }

    @Test
    fun `null server is rejected`() {
        assertRejected(null)
    }

    @Test
    fun `blank server is rejected`() {
        assertRejected("")
        assertRejected("   ")
    }

    @Test
    fun `malformed or ambiguous server is rejected`() {
        listOf(
            "https://",
            "https:///api.mypoopak.ir",
            "https://api.mypoopak.ir:",
            "api.mypoopak.ir:invalid",
            "api.mypoopak.ir:0",
            "api.mypoopak.ir:65536",
            "api.mypoopak.ir:999999999999",
            "api.mypoopak.ir\\evil",
            "api.mypoopak.ir\n",
            " api.mypoopak.ir",
            "https://api.mypoopak.ir/path",
            "https://api.mypoopak.ir//",
            "https://api.mypoopak.ir?query",
            "https://api.mypoopak.ir#fragment",
            "https://user@api.mypoopak.ir",
            "https://api.mypoopak.ir@evil.com",
            "https://%61pi.mypoopak.ir",
            "ftp://api.mypoopak.ir",
        ).forEach { assertRejected(it) }
    }

    @Test
    fun `different explicit port is rejected`() {
        assertRejected("api.mypoopak.ir:8448")
        assertRejected("api.mypoopak.ir:8449", configured = "https://api.mypoopak.ir:8448")
        assertRejected("api.mypoopak.ir", configured = "https://api.mypoopak.ir:8448")
    }

    @Test
    fun `matching explicit port is accepted`() {
        assertAllowed("api.mypoopak.ir:8448", configured = "https://api.mypoopak.ir:8448/")
    }

    @Test
    fun `default ports are normalized`() {
        assertAllowed("api.mypoopak.ir:443")
        assertAllowed("api.mypoopak.ir", configured = "https://api.mypoopak.ir:443")
        assertAllowed("http://api.mypoopak.ir:80", configured = "http://api.mypoopak.ir")
    }

    @Test
    fun `different scheme is rejected`() {
        assertRejected("http://api.mypoopak.ir")
        assertRejected("http://api.mypoopak.ir:443")
    }

    @Test
    fun `missing or malformed configured homeserver fails closed`() {
        listOf(null, "", "https://", "https://api.mypoopak.ir/path").forEach { configured ->
            assertRejected("api.mypoopak.ir", configured)
        }
    }

    @Test
    fun `unreadable QR server fails closed`() {
        val validator = createValidator()
        assertThat(validator.isAllowed(FakeMatrixQrCodeLoginData { error("Unavailable") })).isFalse()
    }

    @Test
    fun `unreadable configured server fails closed`() {
        val validator = QrLoginHomeserverValidator(
            defaultHomeserverProvider = object : DefaultHomeserverProvider {
                override fun getDefaultHomeserver(): String = error("Unavailable")
            },
            canChangeHomeserver = false,
        )
        assertThat(validator.isAllowed(FakeMatrixQrCodeLoginData { "api.mypoopak.ir" })).isFalse()
    }

    @Test
    fun `unrestricted login does not read either homeserver`() {
        val validator = QrLoginHomeserverValidator(
            defaultHomeserverProvider = object : DefaultHomeserverProvider {
                override fun getDefaultHomeserver(): String? = lambdaError()
            },
            canChangeHomeserver = true,
        )
        assertThat(validator.isAllowed(FakeMatrixQrCodeLoginData())).isTrue()
    }

    private fun assertAllowed(server: String?, configured: String? = "https://api.mypoopak.ir") {
        assertThat(createValidator(configured).isAllowed(FakeMatrixQrCodeLoginData { server })).isTrue()
    }

    private fun assertRejected(server: String?, configured: String? = "https://api.mypoopak.ir") {
        assertThat(createValidator(configured).isAllowed(FakeMatrixQrCodeLoginData { server })).isFalse()
    }

    private fun createValidator(configured: String? = "https://api.mypoopak.ir") = QrLoginHomeserverValidator(
        defaultHomeserverProvider = object : DefaultHomeserverProvider {
            override fun getDefaultHomeserver() = configured
        },
        canChangeHomeserver = false,
    )
}
