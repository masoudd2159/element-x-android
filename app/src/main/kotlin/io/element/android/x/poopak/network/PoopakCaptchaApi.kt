/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.poopak.network

import retrofit2.http.GET

internal interface PoopakCaptchaApi {
    @GET("_matrix/client/v3/service/captcha")
    suspend fun getCaptcha(): PoopakCaptchaResponse
}
