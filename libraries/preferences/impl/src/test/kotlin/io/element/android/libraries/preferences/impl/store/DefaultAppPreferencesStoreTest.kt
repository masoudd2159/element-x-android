/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.impl.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.appconfig.AppFont
import io.element.android.appconfig.CustomAppConfig
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.preferences.api.store.MessageTextSize
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory as StoreFactory

class DefaultAppPreferencesStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val buildMeta = BuildMeta(
        buildType = BuildType.DEBUG,
        isDebuggable = true,
        applicationName = "Element X",
        productionApplicationName = "Element",
        desktopApplicationName = "Element Desktop",
        applicationId = "io.element.android",
        isEnterpriseBuild = false,
        lowPrivacyLoggingEnabled = false,
        versionName = "1.0.0",
        versionCode = 1,
        gitRevision = "test",
        gitBranchName = "test",
        flavorDescription = "test",
        flavorShortDescription = "test",
    )

    @Test
    fun `live location minimum distance defaults to 10`() = runTest {
        val store = DefaultAppPreferencesStore(
            buildMeta = buildMeta,
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
        )

        assertThat(store.getLiveLocationMinimumDistanceInMetersUpdateFlow().first()).isEqualTo(10)
    }

    @Test
    fun `live location minimum distance persists updates`() = runTest {
        val store = DefaultAppPreferencesStore(
            buildMeta = buildMeta,
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
        )

        store.setLiveLocationMinimumDistanceInMetersUpdate(25)

        assertThat(store.getLiveLocationMinimumDistanceInMetersUpdateFlow().first()).isEqualTo(25)
    }

    @Test
    fun `the app font defaults to Yekan Bakh`() = runTest {
        assertThat(CustomAppConfig.Appearance.FONT).isEqualTo(AppFont.YEKAN_BAKH)
        assertThat(createStore().getAppFontFlow().first()).isEqualTo(AppFont.YEKAN_BAKH)
    }

    @Test
    fun `app font updates emit and survive reopening the DataStore`() = runTest {
        val file = tempFolder.root.resolve("app.preferences_pb")
        val writerJob = Job()
        val writerDataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(coroutineContext + writerJob)) { file }
        val writer = createStore(writerDataStore)
        try {
            writer.getAppFontFlow().test {
                assertThat(awaitItem()).isEqualTo(AppFont.YEKAN_BAKH)
                writer.setAppFont(AppFont.IRAN_SANS_X)
                assertThat(awaitItem()).isEqualTo(AppFont.IRAN_SANS_X)
                assertThat(writerDataStore.data.first()[stringPreferencesKey("appFont")]).isEqualTo("IRAN_SANS_X")
            }
        } finally {
            writerJob.cancelAndJoin()
        }

        val readerDataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val reader = createStore(readerDataStore)
        reader.getAppFontFlow().test {
            assertThat(awaitItem()).isEqualTo(AppFont.IRAN_SANS_X)
            reader.setAppFont(AppFont.YEKAN_BAKH)
            assertThat(awaitItem()).isEqualTo(AppFont.YEKAN_BAKH)
            assertThat(readerDataStore.data.first()[stringPreferencesKey("appFont")]).isEqualTo("YEKAN_BAKH")
        }
    }

    @Test
    fun `an invalid stored app font falls back to the configured default`() = runTest {
        val dataStore = FakePreferenceDataStoreFactory().create("elementx_preferences")
        dataStore.edit { it[stringPreferencesKey("appFont")] = "UNKNOWN_FONT" }
        val store = createStore(dataStore)

        assertThat(store.getAppFontFlow().first()).isEqualTo(CustomAppConfig.Appearance.FONT)
    }

    @Test
    fun `a transient app font read error recovers the saved font`() = runTest {
        var readAttempts = 0
        val dataStore = FakePreferenceDataStoreFactory().create("elementx_preferences")
        val store = createStore(dataStore.beforeRead {
            if (++readAttempts == 1) throw IOException("Temporary read failure")
        })
        store.setAppFont(AppFont.IRAN_SANS_X)

        assertThat(store.getAppFontFlow().first()).isEqualTo(AppFont.IRAN_SANS_X)
        assertThat(readAttempts).isEqualTo(2)
    }

    @Test
    fun `persistent app font read errors fall back and a later collection can recover`() = runTest {
        var readAttempts = 0
        var failReads = true
        val dataStore = FakePreferenceDataStoreFactory().create("elementx_preferences")
        val store = createStore(dataStore.beforeRead {
            readAttempts++
            if (failReads) throw IOException("Persistent read failure")
        })
        store.setAppFont(AppFont.IRAN_SANS_X)
        val appFontFlow = store.getAppFontFlow()

        appFontFlow.test {
            assertThat(awaitItem()).isEqualTo(AppFont.YEKAN_BAKH)
            awaitComplete()
        }
        assertThat(readAttempts).isEqualTo(3)

        failReads = false
        assertThat(appFontFlow.first()).isEqualTo(AppFont.IRAN_SANS_X)
        assertThat(readAttempts).isEqualTo(4)
    }

    @Test
    fun `unexpected app font read errors propagate without retries`() = runTest {
        var readAttempts = 0
        val failure = IllegalStateException("Unexpected read failure")
        val dataStore = FakePreferenceDataStoreFactory().create("elementx_preferences")
        val store = createStore(dataStore.beforeRead {
            readAttempts++
            throw failure
        })

        store.getAppFontFlow().test {
            assertThat(awaitError()).isSameInstanceAs(failure)
        }
        assertThat(readAttempts).isEqualTo(1)
    }

    @Test
    fun `reset restores the default app font`() = runTest {
        val store = createStore()
        store.setAppFont(AppFont.IRAN_SANS_X)
        store.reset()

        assertThat(store.getAppFontFlow().first()).isEqualTo(CustomAppConfig.Appearance.FONT)
    }

    @Test
    fun `message text size defaults to 16`() = runTest {
        val store = createStore()

        assertThat(store.getMessageTextSizeFlow().first()).isEqualTo(MessageTextSize.DEFAULT)
    }

    @Test
    fun `message text size persists updates`() = runTest {
        val store = createStore()

        store.setMessageTextSize(20)

        assertThat(store.getMessageTextSizeFlow().first()).isEqualTo(20)
    }

    @Test
    fun `message text size is coerced into the supported range`() = runTest {
        val store = createStore()

        store.setMessageTextSize(99)

        assertThat(store.getMessageTextSizeFlow().first()).isEqualTo(MessageTextSize.MAX)
    }

    @Test
    fun `homeserver history is empty by default`() = runTest {
        val store = createStore()
        assertThat(store.getHomeserverHistoryFlow().first()).isEmpty()
    }

    @Test
    fun `adding homeservers keeps the most recent first`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("https://matrix.org")
        store.addHomeserverToHistory("https://example.com")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://example.com", "https://matrix.org")
            .inOrder()
    }

    @Test
    fun `adding an existing homeserver moves it to the front without duplicating`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("https://matrix.org")
        store.addHomeserverToHistory("https://example.com")
        store.addHomeserverToHistory("https://matrix.org")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://matrix.org", "https://example.com")
            .inOrder()
    }

    @Test
    fun `homeservers are normalised and deduplicated case-insensitively`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("  HTTPS://Matrix.ORG  ")
        store.addHomeserverToHistory("https://matrix.org")

        assertThat(store.getHomeserverHistoryFlow().first())
            .containsExactly("https://matrix.org")
    }

    @Test
    fun `blank homeservers are ignored`() = runTest {
        val store = createStore()

        store.addHomeserverToHistory("   ")

        assertThat(store.getHomeserverHistoryFlow().first()).isEmpty()
    }

    @Test
    fun `homeserver history is capped to the most recent entries`() = runTest {
        val store = createStore()

        repeat(25) { index ->
            store.addHomeserverToHistory("https://server$index.org")
        }

        val history = store.getHomeserverHistoryFlow().first()
        assertThat(history).hasSize(20)
        assertThat(history.first()).isEqualTo("https://server24.org")
        assertThat(history.last()).isEqualTo("https://server5.org")
    }

    @Test
    fun `other accounts section is expanded by default`() = runTest {
        val store = createStore()

        assertThat(store.isOtherAccountsExpandedFlow().first()).isTrue()
    }

    @Test
    fun `other accounts expanded state persists updates`() = runTest {
        val store = createStore()

        store.setOtherAccountsExpanded(false)
        assertThat(store.isOtherAccountsExpandedFlow().first()).isFalse()

        store.setOtherAccountsExpanded(true)
        assertThat(store.isOtherAccountsExpandedFlow().first()).isTrue()
    }

    private fun DataStore<Preferences>.beforeRead(action: () -> Unit): DataStore<Preferences> =
        object : DataStore<Preferences> by this {
            override val data = flow {
                action()
                emitAll(this@beforeRead.data)
            }
        }

    private fun createStore(dataStore: DataStore<Preferences>) = DefaultAppPreferencesStore(
        buildMeta = buildMeta,
        preferenceDataStoreFactory = object : StoreFactory {
            override fun create(name: String): DataStore<Preferences> = dataStore
        },
    )

    private fun createStore() = DefaultAppPreferencesStore(
        buildMeta = buildMeta,
        preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
    )
}
