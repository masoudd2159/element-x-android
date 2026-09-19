/*
 * Copyright (c) 2026 MyPoopak
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
    namespace = "ir.mypoopak.android.brandconfig"
}

setupDependencyInjection()

dependencies {
    implementation(projects.libraries.di)
    implementation(projects.libraries.pushproviders.firebase)

    testCommonDependencies(libs)
}
