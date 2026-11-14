/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.poopak.network

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.network.interceptors.UserAgentInterceptor
import io.element.android.libraries.network.useragent.UserAgentProvider
import io.element.android.x.BuildConfig
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.TimeUnit

class PoopakCaptchaApiFactoryTest {
    private fun factory() = PoopakCaptchaApiFactory(
        UserAgentInterceptor(object : UserAgentProvider {
            override fun provide() = "test-user-agent"
        }),
        DefaultJsonProvider(),
    )

    @Test
    fun `hostname and HTTPS origin normalize to a root URL`() {
        assertThat(captchaBackendUrl("example.test:8448").toString()).isEqualTo("https://example.test:8448/")
        assertThat(captchaBackendUrl("https://example.test:8448/").toString()).isEqualTo("https://example.test:8448/")
    }

    @Test
    fun `invalid backend origins are rejected`() {
        listOf(
            "", " ", "http://example.test", "ftp://example.test", "https://", "https://example.test:abc", "https://example.test:",
            "https://user:password@example.test", "https://@example.test", "https://example.test/path",
            "https://example.test/..", "https://example.test?query=1", "https://example.test#fragment",
            "https://example.test?", "https://example.test#", "https://example.test\\evil.test",
        ).forEach { input ->
            assertThrows("Should reject invalid backend origin", IllegalArgumentException::class.java) { captchaBackendUrl(input) }
        }
    }

    @Test
    fun `factory selects flavor backend and resolves endpoint at root`() {
        val factory = factory()
        assertThat(factory.baseUrl).isEqualTo(captchaBackendUrl(BuildConfig.BACKEND_HOST))
        assertThat(factory.baseUrl.resolve("_matrix/client/v3/service/captcha")?.encodedPath)
            .isEqualTo("/_matrix/client/v3/service/captcha")
        assertThat(factory.create()).isNotNull()
    }

    @Test
    fun `dedicated client sends user agent without authorization and does not follow redirects`() {
        val client = factory().client
        assertThat(client.interceptors).hasSize(1)
        assertThat(client.interceptors.single()).isInstanceOf(UserAgentInterceptor::class.java)
        assertThat(client.networkInterceptors).isEmpty()
        assertThat(client.followRedirects).isFalse()
        assertThat(client.followSslRedirects).isFalse()

        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/other"))
            server.start()
            client.newCall(Request.Builder().url(server.url("/captcha")).build()).execute().use { response ->
                assertThat(response.code).isEqualTo(302)
            }
            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertThat(request.getHeader("Authorization")).isNull()
            assertThat(request.getHeader("User-Agent")).isEqualTo("test-user-agent")
            assertThat(server.requestCount).isEqualTo(1)
        }
    }
}
