/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package poopak.android.brandconfig.locale

import android.content.Context
import androidx.startup.Initializer

class DefaultLocaleInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        PoopakAppLocaleManager.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
