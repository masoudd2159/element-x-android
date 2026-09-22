/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.accountprovider

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.appconfig.CustomAppConfig
import io.element.android.appconfig.DefaultHomeserverProvider
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.matrix.api.accountprovider.AccountProvider
import io.element.android.libraries.matrix.api.accountprovider.matrixOrgAccountProvider
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@Inject
class AccountProviderDataSource(
    private val enterpriseService: EnterpriseService,
    private val defaultHomeserverProvider: DefaultHomeserverProvider,
    private val appPreferencesStore: AppPreferencesStore,
    @AppCoroutineScope private val coroutineScope: CoroutineScope,
) {
    private val enterpriseAccountProvider = enterpriseService.accountProviderAllowList().firstOrNull()
    private val brandAccountProvider = defaultHomeserverProvider.getDefaultHomeserver()?.let(AccountProvider::Generic)
    private val configuredAccountProvider = when {
        !enterpriseService.canConnectToAnyAccountProvider() -> enterpriseAccountProvider ?: matrixOrgAccountProvider
        brandAccountProvider != null -> brandAccountProvider
        else -> enterpriseAccountProvider ?: matrixOrgAccountProvider
    }

    private val accountProvider: MutableStateFlow<AccountProvider> = MutableStateFlow(configuredAccountProvider)

    val flow: StateFlow<AccountProvider> = accountProvider.asStateFlow()

    // The account provider the user last explicitly selected (via [setAccountProvider] / [setUrl]).
    // Unlike [flow], this is not recomputed by [reset], so it survives to be persisted to history on a
    // successful sign-in even when the login flow is torn down in between (e.g. across an OAuth round-trip).
    var lastSelectedAccountProvider: AccountProvider? = null
        private set

    init {
        // Seed the default from the last used provider, unless the user has already selected one.
        coroutineScope.launch {
            val default = defaultAccountProvider()
            accountProvider.update { current -> if (current == configuredAccountProvider) default else current }
        }
    }

    suspend fun reset() {
        accountProvider.emit(defaultAccountProvider())
    }

    /**
     * The provider to default to: an enforced enterprise/MDM provider, then the brand provider,
     * then the most recently used provider from history, and finally the configured fallback.
     */
    private suspend fun defaultAccountProvider(): AccountProvider {
        if (!CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER) {
            return configuredAccountProvider
        }
        if (!enterpriseService.canConnectToAnyAccountProvider()) {
            return configuredAccountProvider
        }
        brandAccountProvider?.let { return it }
        val lastUsedProvider = appPreferencesStore.getHomeserverHistoryFlow().first().firstOrNull()
        return lastUsedProvider?.let { AccountProvider.Generic(it) } ?: configuredAccountProvider
    }

    suspend fun setUrl(url: String) {
        setAccountProvider(AccountProvider.Generic(url))
    }

    suspend fun setAccountProvider(data: AccountProvider) {
        lastSelectedAccountProvider = data
        accountProvider.emit(data)
    }
}
