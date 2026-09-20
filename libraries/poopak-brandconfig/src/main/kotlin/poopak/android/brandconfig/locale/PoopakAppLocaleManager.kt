/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package poopak.android.brandconfig.locale

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

sealed interface PoopakApplicationLocale {
    data object SystemDefault : PoopakApplicationLocale

    data class Language(val languageTag: String) : PoopakApplicationLocale
}

object PoopakAppLocaleManager {
    /**
     * Returns a context configured with the persisted application locale on API 28-32.
     * API 33+ is configured by the platform's [LocaleManager].
     */
    fun localizedContext(base: Context): Context {
        val applicationLocale = initialize(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || applicationLocale is PoopakApplicationLocale.SystemDefault) {
            return base
        }
        return createLocalizedContext(base, (applicationLocale as PoopakApplicationLocale.Language).languageTag)
    }

    /** The current application locale, initializing the Persian default when needed. */
    fun currentApplicationLocale(context: Context): PoopakApplicationLocale = initialize(context)

    /** Persists an explicit locale. The current Activity should be recreated after this call on API 28-32. */
    fun setApplicationLanguage(context: Context, languageTag: String) {
        val normalizedLanguageTag = normalizedLanguageTag(languageTag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33ApplicationLocales.set(context, normalizedLanguageTag)
        }
        applicationLocaleStore(context).write(StoredApplicationLocale.Language(normalizedLanguageTag))
    }

    /** Persists System default. The current Activity should be recreated after this call on API 28-32. */
    fun useSystemLanguage(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33ApplicationLocales.useSystem(context)
        }
        applicationLocaleStore(context).write(StoredApplicationLocale.SystemDefault)
    }

    internal fun initialize(context: Context): PoopakApplicationLocale {
        val platformLocale = platformApplicationLocale(context)
        val store = applicationLocaleStore(context)
        val persistedLocale = store.read(platformLocale)
        val result = initializeDefaultLocale(persistedLocale, platformLocale)

        result.platformLanguageToApply?.let { languageTag ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33ApplicationLocales.set(context, languageTag)
            }
        }
        if (result.persistedLocale != persistedLocale || store.hasLegacyInitializationMarker()) {
            store.write(result.persistedLocale)
        }
        return result.persistedLocale.toPublicLocale()
    }

    private fun createLocalizedContext(base: Context, languageTag: String): Context {
        val locales = LocaleList.forLanguageTags(languageTag)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(locales)
            setLayoutDirection(locales[0])
        }
        return base.createConfigurationContext(configuration)
    }

    private fun normalizedLanguageTag(languageTag: String): String {
        val locales = LocaleList.forLanguageTags(languageTag)
        require(!locales.isEmpty) { "languageTag must identify a locale" }
        return locales[0].toLanguageTag()
    }

    private fun platformApplicationLocale(context: Context): PlatformApplicationLocale {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PlatformApplicationLocale.Unsupported
        }
        return Api33ApplicationLocales.current(context)
    }

    private fun applicationLocaleStore(context: Context): SharedPreferencesApplicationLocaleStore {
        return SharedPreferencesApplicationLocaleStore(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object Api33ApplicationLocales {
    fun current(context: Context): PlatformApplicationLocale {
        val locales = localeManager(context).applicationLocales
        return if (locales.isEmpty) {
            PlatformApplicationLocale.SystemDefault
        } else {
            PlatformApplicationLocale.Language(locales[0].toLanguageTag())
        }
    }

    fun set(context: Context, languageTag: String) {
        localeManager(context).applicationLocales = LocaleList.forLanguageTags(languageTag)
    }

    fun useSystem(context: Context) {
        localeManager(context).applicationLocales = LocaleList.getEmptyLocaleList()
    }

    private fun localeManager(context: Context): LocaleManager = context.getSystemService(LocaleManager::class.java)
}

private class SharedPreferencesApplicationLocaleStore(
    private val preferences: SharedPreferences,
) {
    fun read(platformLocale: PlatformApplicationLocale): StoredApplicationLocale {
        val storedValue = preferences.getString(APPLICATION_LOCALE_KEY, null)
        if (storedValue != null) {
            return storedValue.toStoredLocale()
        }
        if (hasLegacyInitializationMarker()) {
            return when (platformLocale) {
                PlatformApplicationLocale.Unsupported,
                PlatformApplicationLocale.SystemDefault,
                -> StoredApplicationLocale.SystemDefault
                is PlatformApplicationLocale.Language -> StoredApplicationLocale.Language(platformLocale.languageTag)
            }
        }
        return StoredApplicationLocale.Uninitialized
    }

    fun hasLegacyInitializationMarker(): Boolean = preferences.contains(LEGACY_INITIALIZED_KEY)

    fun write(applicationLocale: StoredApplicationLocale) {
        val storedValue = when (applicationLocale) {
            StoredApplicationLocale.Uninitialized -> null
            StoredApplicationLocale.SystemDefault -> SYSTEM_DEFAULT_VALUE
            is StoredApplicationLocale.Language -> LANGUAGE_VALUE_PREFIX + applicationLocale.languageTag
        }
        preferences.edit()
            .putString(APPLICATION_LOCALE_KEY, storedValue)
            .remove(LEGACY_INITIALIZED_KEY)
            .commit()
    }

    private fun String.toStoredLocale(): StoredApplicationLocale {
        return when {
            this == SYSTEM_DEFAULT_VALUE -> StoredApplicationLocale.SystemDefault
            startsWith(LANGUAGE_VALUE_PREFIX) && length > LANGUAGE_VALUE_PREFIX.length -> {
                StoredApplicationLocale.Language(removePrefix(LANGUAGE_VALUE_PREFIX))
            }
            else -> StoredApplicationLocale.SystemDefault
        }
    }
}

private fun StoredApplicationLocale.toPublicLocale(): PoopakApplicationLocale {
    return when (this) {
        StoredApplicationLocale.Uninitialized -> error("Application locale must be initialized")
        StoredApplicationLocale.SystemDefault -> PoopakApplicationLocale.SystemDefault
        is StoredApplicationLocale.Language -> PoopakApplicationLocale.Language(languageTag)
    }
}

private const val PREFERENCES_NAME = "poopak_brand_config"
private const val APPLICATION_LOCALE_KEY = "poopak_application_locale"
private const val LEGACY_INITIALIZED_KEY = "poopak_default_locale_initialized"
private const val SYSTEM_DEFAULT_VALUE = "system"
private const val LANGUAGE_VALUE_PREFIX = "language:"
