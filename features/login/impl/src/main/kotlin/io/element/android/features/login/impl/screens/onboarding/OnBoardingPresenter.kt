/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.appconfig.CustomAppConfig
import io.element.android.appconfig.OnBoardingConfig
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.features.login.impl.accesscontrol.DefaultAccountProviderAccessControl
import io.element.android.features.login.impl.accountprovider.AccountProviderDataSource
import io.element.android.features.login.impl.login.LoginModeEvent
import io.element.android.features.login.impl.login.LoginModeState
import io.element.android.features.rageshake.api.RageshakeFeatureAvailability
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.matrix.api.accountprovider.AccountProvider
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.ui.utils.MultipleTapToUnlock
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class OnBoardingPresenter(
    @Assisted private val params: OnBoardingNode.Params,
    private val buildMeta: BuildMeta,
    private val enterpriseService: EnterpriseService,
    private val defaultAccountProviderAccessControl: DefaultAccountProviderAccessControl,
    private val rageshakeFeatureAvailability: RageshakeFeatureAvailability,
    private val loginModePresenter: Presenter<LoginModeState>,
    private val onBoardingLogoResIdProvider: OnBoardingLogoResIdProvider,
    private val sessionStore: SessionStore,
    private val accountProviderDataSource: AccountProviderDataSource,
) : Presenter<OnBoardingState> {
    @AssistedFactory
    interface Factory {
        fun create(
            params: OnBoardingNode.Params,
        ): OnBoardingPresenter
    }

    private val multipleTapToUnlock = MultipleTapToUnlock()

    @Composable
    override fun present(): OnBoardingState {
        val localCoroutineScope = rememberCoroutineScope()
        val configuredAccountProvider by accountProviderDataSource.flow.collectAsState()
        val canConnectToAnyAccountProvider = remember {
            enterpriseService.canConnectToAnyAccountProvider()
        }
        val forcedAccountProvider = remember {
            // If accountProviderAllowList() returns a singleton list, and the user is not free to use another
            // account provider, this is the default account provider.
            // In this case, the user can sign in using this homeserver, or use QrCode login
            if (canConnectToAnyAccountProvider) {
                null
            } else {
                enterpriseService.accountProviderAllowList().singleOrNull()
            }
        }
        val mustChooseAccountProvider = remember {
            CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER &&
                !canConnectToAnyAccountProvider && enterpriseService.accountProviderAllowList().size > 1
        }
        val linkAccountProvider by produceState<AccountProvider?>(initialValue = null) {
            // Account provider from the link, if allowed by the enterprise service
            value = params.accountProvider
                ?.let { AccountProvider.Generic(it) }
                ?.takeIf {
                    try {
                        defaultAccountProviderAccessControl.assertIsAllowedToConnectToAccountProvider(it)
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to check account provider from link, assuming not allowed")
                        false
                    }
                }
        }
        val defaultAccountProvider = remember(linkAccountProvider, configuredAccountProvider) {
            // When homeserver changes are disabled, use the application's configured provider.
            // Otherwise retain the upstream forced-provider and link behavior.
            if (CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER) {
                forcedAccountProvider ?: linkAccountProvider
            } else {
                forcedAccountProvider ?: configuredAccountProvider
            }
        }
        val canLoginWithQrCode by produceState(initialValue = false, linkAccountProvider) {
            value = CustomAppConfig.FeatureFlags.QR_LOGIN &&
                (!CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER || linkAccountProvider == null)
        }
        val canReportBug by remember { rageshakeFeatureAvailability.isAvailable() }.collectAsState(false)
        var showReportBug by rememberSaveable { mutableStateOf(false) }
        val onBoardingLogoResId = remember {
            onBoardingLogoResIdProvider.get()
        }
        val isAddingAccount by produceState(initialValue = false) {
            // We are adding an account if there is at least one session already stored
            value = sessionStore.numberOfSessions() > 0
        }

        val loginModeState = loginModePresenter.present()

        fun submit(defaultAccountProvider: AccountProvider, isAccountCreation: Boolean) = localCoroutineScope.launch {
            // Ensure that the current account provider is set
            accountProviderDataSource.setAccountProvider(defaultAccountProvider)
            loginModeState.eventSink(
                LoginModeEvent.Submit(
                    isAccountCreation = isAccountCreation,
                    homeserverUrl = defaultAccountProvider.serverNameOrBaseUrl(),
                    resolvedHomeserverUrl = null,
                    loginHint = params.loginHint?.takeIf {
                        !isAccountCreation &&
                            CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER && forcedAccountProvider == null
                    },
                )
            )
        }

        fun handleEvent(event: OnBoardingEvent) {
            when (event) {
                is OnBoardingEvent.OnSignIn -> submit(event.defaultAccountProvider, isAccountCreation = false)
                is OnBoardingEvent.OnCreateAccount -> submit(event.defaultAccountProvider, isAccountCreation = true)
                OnBoardingEvent.ClearError -> loginModeState.eventSink(LoginModeEvent.ClearError)
                OnBoardingEvent.OnVersionClick -> {
                    if (canReportBug) {
                        if (multipleTapToUnlock.unlock(localCoroutineScope)) {
                            showReportBug = true
                        }
                    }
                }
            }
        }

        return OnBoardingState(
            isAddingAccount = isAddingAccount,
            showBackButton = params.showBackButton,
            showDeveloperSettings = buildMeta.buildType != BuildType.RELEASE,
            productionApplicationName = buildMeta.productionApplicationName,
            defaultAccountProvider = defaultAccountProvider,
            mustChooseAccountProvider = mustChooseAccountProvider,
            canLoginWithQrCode = canLoginWithQrCode,
            canCreateAccount = OnBoardingConfig.CAN_CREATE_ACCOUNT &&
                if (CustomAppConfig.FeatureFlags.CHANGE_HOMESERVER) {
                    defaultAccountProvider == null && canConnectToAnyAccountProvider
                } else {
                    defaultAccountProvider != null
                },
            canReportBug = canReportBug && showReportBug,
            loginModeState = loginModeState,
            version = buildMeta.versionName,
            onBoardingLogoResId = onBoardingLogoResId,
            eventSink = ::handleEvent,
        )
    }
}
