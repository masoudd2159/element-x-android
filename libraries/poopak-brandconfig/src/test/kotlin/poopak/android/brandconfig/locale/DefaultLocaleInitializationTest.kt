/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package poopak.android.brandconfig.locale

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultLocaleInitializationTest {
    @Test
    fun `fresh install selects Persian without framework locale support`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Uninitialized,
            platformLocale = PlatformApplicationLocale.Unsupported,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language(DEFAULT_LANGUAGE_TAG))
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `existing Persian is preserved`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Uninitialized,
            platformLocale = PlatformApplicationLocale.Language("fa"),
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language("fa"))
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `existing English is preserved`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Uninitialized,
            platformLocale = PlatformApplicationLocale.Language("en"),
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language("en"))
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `fresh native locale state applies Persian`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Uninitialized,
            platformLocale = PlatformApplicationLocale.SystemDefault,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language("fa"))
        assertThat(result.platformLanguageToApply).isEqualTo("fa")
    }

    @Test
    fun `user selected System default is preserved`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.SystemDefault,
            platformLocale = PlatformApplicationLocale.Unsupported,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.SystemDefault)
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `restart after selecting English preserves English`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Language("en"),
            platformLocale = PlatformApplicationLocale.Unsupported,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language("en"))
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `restart after selecting System default preserves System default`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.SystemDefault,
            platformLocale = PlatformApplicationLocale.Unsupported,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.SystemDefault)
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `repeated native initialization does not reapply Persian`() {
        val firstResult = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Uninitialized,
            platformLocale = PlatformApplicationLocale.SystemDefault,
        )
        val secondResult = initializeDefaultLocale(
            persistedLocale = firstResult.persistedLocale,
            platformLocale = PlatformApplicationLocale.Language("fa"),
        )

        assertThat(firstResult.platformLanguageToApply).isEqualTo("fa")
        assertThat(secondResult.platformLanguageToApply).isNull()
    }

    @Test
    fun `native English change replaces previously persisted Persian`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Language("fa"),
            platformLocale = PlatformApplicationLocale.Language("en"),
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.Language("en"))
        assertThat(result.platformLanguageToApply).isNull()
    }

    @Test
    fun `native System change replaces previously persisted language`() {
        val result = initializeDefaultLocale(
            persistedLocale = StoredApplicationLocale.Language("de"),
            platformLocale = PlatformApplicationLocale.SystemDefault,
        )

        assertThat(result.persistedLocale).isEqualTo(StoredApplicationLocale.SystemDefault)
        assertThat(result.platformLanguageToApply).isNull()
    }
}
