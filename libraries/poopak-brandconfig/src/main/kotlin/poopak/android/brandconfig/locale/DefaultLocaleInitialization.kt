/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package poopak.android.brandconfig.locale

internal const val DEFAULT_LANGUAGE_TAG = "fa"

internal sealed interface StoredApplicationLocale {
    data object Uninitialized : StoredApplicationLocale

    data object SystemDefault : StoredApplicationLocale

    data class Language(val languageTag: String) : StoredApplicationLocale
}

internal sealed interface PlatformApplicationLocale {
    data object Unsupported : PlatformApplicationLocale

    data object SystemDefault : PlatformApplicationLocale

    data class Language(val languageTag: String) : PlatformApplicationLocale
}

internal data class DefaultLocaleInitializationResult(
    val persistedLocale: StoredApplicationLocale,
    val platformLanguageToApply: String? = null,
)

internal fun initializeDefaultLocale(
    persistedLocale: StoredApplicationLocale,
    platformLocale: PlatformApplicationLocale,
): DefaultLocaleInitializationResult {
    return when (persistedLocale) {
        StoredApplicationLocale.Uninitialized -> when (platformLocale) {
            PlatformApplicationLocale.Unsupported -> DefaultLocaleInitializationResult(
                persistedLocale = StoredApplicationLocale.Language(DEFAULT_LANGUAGE_TAG),
            )
            PlatformApplicationLocale.SystemDefault -> DefaultLocaleInitializationResult(
                persistedLocale = StoredApplicationLocale.Language(DEFAULT_LANGUAGE_TAG),
                platformLanguageToApply = DEFAULT_LANGUAGE_TAG,
            )
            is PlatformApplicationLocale.Language -> DefaultLocaleInitializationResult(
                persistedLocale = StoredApplicationLocale.Language(platformLocale.languageTag),
            )
        }
        StoredApplicationLocale.SystemDefault,
        is StoredApplicationLocale.Language,
        -> when (platformLocale) {
            PlatformApplicationLocale.Unsupported -> DefaultLocaleInitializationResult(persistedLocale)
            PlatformApplicationLocale.SystemDefault -> DefaultLocaleInitializationResult(StoredApplicationLocale.SystemDefault)
            is PlatformApplicationLocale.Language -> DefaultLocaleInitializationResult(
                persistedLocale = StoredApplicationLocale.Language(platformLocale.languageTag),
            )
        }
    }
}
