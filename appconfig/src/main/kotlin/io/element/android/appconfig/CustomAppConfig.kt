/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

/**
 * Central configuration for custom application behavior.
 *
 * Keep fork-specific configuration here instead of scattering
 * custom values across upstream Element X files.
 */
object CustomAppConfig {
    object Appearance {
        /**
         * The application-wide font. Change only this value to switch fonts.
         */
        val FONT = AppFont.YEKAN_BAKH
    }

    object FeatureFlags {
        /**
         * Controls whether the "Create account" option is available in the app.
         *
         * Account registration must still be controlled by the
         * homeserver / authentication server.
         */
        const val CREATE_ACCOUNT = false

        /**
         * Controls whether users are allowed to manually select,
         * enter, or change their Homeserver / account provider.
         *
         * When false, authentication must continue using the
         * application's configured/default Homeserver.
         */
        const val CHANGE_HOMESERVER = false

        const val QR_LOGIN = true
    }
}
