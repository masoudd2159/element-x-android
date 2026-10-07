/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.text.TextStyle
import io.element.android.compound.tokens.ElementTypography
import io.element.android.compound.tokens.generated.TypographyTokens

// Keep upstream's alias definitions authoritative; replace only the configured font family.
val ElementTypography.aliasScreenTitle: TextStyle
    get() = TypographyTokens.aliasScreenTitle.copy(fontFamily = fontBodyLgRegular.fontFamily)

val ElementTypography.aliasButtonText: TextStyle
    get() = TypographyTokens.aliasButtonText.copy(fontFamily = fontBodyLgRegular.fontFamily)
