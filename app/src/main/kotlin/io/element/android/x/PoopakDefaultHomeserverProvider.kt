/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.appconfig.DefaultHomeserverProvider

@ContributesBinding(AppScope::class)
class PoopakDefaultHomeserverProvider : DefaultHomeserverProvider {
    override fun getDefaultHomeserver(): String? = normalizeHomeserver(BuildConfig.BACKEND_HOST)
}

internal fun normalizeHomeserver(value: String): String? {
    val homeserver = value.trim().trimEnd('/')
    if (homeserver.isEmpty()) return null
    return if (homeserver.startsWith("https://", ignoreCase = true) || homeserver.startsWith("http://", ignoreCase = true)) {
        homeserver
    } else {
        "https://$homeserver"
    }
}
