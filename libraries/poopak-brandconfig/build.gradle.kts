/*
 * Copyright (c) 2026 Poopak
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import extension.setupDependencyInjection
import extension.testCommonDependencies

plugins {
    id("io.element.android-library")
}

android {
    namespace = "poopak.android.brandconfig"
}

setupDependencyInjection()

dependencies {
    implementation(projects.libraries.di)
    implementation(projects.libraries.pushproviders.firebase)
    implementation(libs.androidx.startup)

    testCommonDependencies(libs)
}
