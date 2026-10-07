/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.qrcode

import dev.zacsweers.metro.Inject
import io.element.android.appconfig.CustomAppConfig
import io.element.android.appconfig.DefaultHomeserverProvider
import io.element.android.libraries.matrix.api.auth.qrlogin.MatrixQrCodeLoginData
import java.net.URI
import java.util.Locale

class QrLoginHomeserverValidator internal constructor(
    private val defaultHomeserverProvider: DefaultHomeserverProvider,
    private val canChangeHomeserver: Boolean,
) {
    @Inject
    constructor(defaultHomeserverProvider: DefaultHomeserverProvider) : this(
        defaultHomeserverProvider,
        CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER,
    )

    fun isAllowed(qrCodeLoginData: MatrixQrCodeLoginData): Boolean {
        if (canChangeHomeserver) return true
        return runCatching {
            val configuredOrigin = canonicalOrigin(defaultHomeserverProvider.getDefaultHomeserver())
            configuredOrigin != null && configuredOrigin == canonicalOrigin(qrCodeLoginData.serverName())
        }.getOrDefault(false)
    }

    private fun canonicalOrigin(value: String?): Origin? {
        if (value.isNullOrBlank() || value.any { it.isWhitespace() }) return null
        // Matrix server names may omit the scheme. Treat those consistently as HTTPS.
        val uri = URI(if (value.contains("://")) value else "https://$value")
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "https" && scheme != "http") return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return null
        if (uri.rawAuthority.endsWith(':')) return null
        val port = when (uri.port) {
            -1 -> if (scheme == "https") 443 else 80
            in 1..65535 -> uri.port
            else -> return null
        }
        return Origin(scheme, host, port)
    }

    private data class Origin(val scheme: String, val host: String, val port: Int)
}
