/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.poopak.network

import dev.zacsweers.metro.Inject
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.network.RetrofitFactory
import io.element.android.libraries.network.interceptors.UserAgentInterceptor
import io.element.android.x.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.net.URI
import java.util.concurrent.TimeUnit

@Inject
internal class PoopakCaptchaApiFactory(
    userAgentInterceptor: UserAgentInterceptor,
    private val jsonProvider: JsonProvider,
) {
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(userAgentInterceptor)
        .build()

    internal val baseUrl: HttpUrl = captchaBackendUrl(BuildConfig.BACKEND_HOST)

    fun create(): PoopakCaptchaApi = RetrofitFactory({ client }, { jsonProvider })
        .create(baseUrl.toString())
        .create(PoopakCaptchaApi::class.java)
}

internal fun captchaBackendUrl(host: String): HttpUrl {
    val value = host.trim()
    require(value.isNotEmpty()) { "Invalid CAPTCHA backend URL" }
    val url = if (value.contains("://")) value else "https://$value"
    val uri = runCatching { URI(url) }.getOrNull()
    require(
        uri != null && uri.scheme.equals("https", ignoreCase = true) && uri.host != null &&
            uri.rawAuthority != null && '@' !in uri.rawAuthority && !uri.rawAuthority.endsWith(':') &&
            (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") && uri.rawQuery == null && uri.rawFragment == null
    ) { "Invalid CAPTCHA backend URL" }
    return requireNotNull(url.toHttpUrlOrNull()) { "Invalid CAPTCHA backend URL" }
}
