/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.poopak.network

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.network.RetrofitFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class PoopakCaptchaApiTest {
    private val server = MockWebServer()
    private lateinit var api: PoopakCaptchaApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        api = RetrofitFactory({ client }, { DefaultJsonProvider() })
            .create(server.url("/").toString())
            .create(PoopakCaptchaApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `enabled response is decoded and request is public`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"captcha_id":"example-id","data":"aW1hZ2U="}"""))

        assertThat(api.getCaptcha()).isEqualTo(PoopakCaptchaResponse("example-id", "aW1hZ2U="))
        val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/_matrix/client/v3/service/captcha")
        assertThat(request.getHeader("Authorization")).isNull()
    }

    @Test
    fun `disabled response accepts null fields`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"captcha_id":null,"data":null}"""))

        assertThat(api.getCaptcha()).isEqualTo(PoopakCaptchaResponse(null, null))
    }

    @Test
    fun `malformed response fails to decode`() {
        server.enqueue(MockResponse().setBody("{"))

        assertThrows(SerializationException::class.java) { runBlocking { api.getCaptcha() } }
    }

    @Test
    fun `HTTP error is propagated`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val error = assertThrows(HttpException::class.java) { runBlocking { api.getCaptcha() } }
        assertThat(error.code()).isEqualTo(503)
    }

    @Test
    fun `network failure is propagated`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(IOException::class.java) { runBlocking { api.getCaptcha() } }
    }
}
