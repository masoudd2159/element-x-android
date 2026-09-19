/*
 * Copyright (c) 2026 MyPoopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package ir.mypoopak.android.brandconfig

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.pushproviders.firebase.DefaultFirebaseGatewayProvider
import io.element.android.libraries.pushproviders.firebase.FirebaseGatewayProvider

@ContributesBinding(
    AppScope::class,
    replaces = [DefaultFirebaseGatewayProvider::class],
)
class BrandFirebaseGatewayProvider(
    @ApplicationContext private val context: Context,
) : FirebaseGatewayProvider {
    override fun getFirebaseGateway(): String {
        return context.getString(R.string.brand_firebase_push_gateway)
    }
}
