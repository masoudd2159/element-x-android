/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoginFlowNodeTest {
    @Test
    fun `disabled account creation redirects a restored account creation target to onboarding`() {
        val target = LoginFlowNode.NavTarget.ConfirmAccountProvider(isAccountCreation = true)

        assertThat(target.withFeatureFlagsApplied()).isEqualTo(LoginFlowNode.NavTarget.OnBoarding(showBackButton = false))
    }

    @Test
    fun `disabled homeserver changes redirect restored server selection targets to onboarding`() {
        assertThat(LoginFlowNode.NavTarget.ChooseAccountProvider.withFeatureFlagsApplied())
            .isEqualTo(LoginFlowNode.NavTarget.OnBoarding(showBackButton = false))
        assertThat(LoginFlowNode.NavTarget.ConfirmAccountProvider(isAccountCreation = false).withFeatureFlagsApplied())
            .isEqualTo(LoginFlowNode.NavTarget.OnBoarding(showBackButton = false))
    }

    @Test
    fun `enabled QR login preserves a restored QR target`() {
        assertThat(LoginFlowNode.NavTarget.QrCode.withFeatureFlagsApplied()).isEqualTo(LoginFlowNode.NavTarget.QrCode)
    }
}
