/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import extension.VerifyPersianTranslationsTask
import extension.resolvedProjectPaths

plugins {
    id("com.android.application")
}

// Keep cross-module translation validation in custom infrastructure. No upstream
// default strings are copied into the app, and lint remains enabled everywhere.
val defaultTranslationResources = rootProject.fileTree(rootProject.projectDir) {
    include("**/src/main/res/values/*.xml")
    exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/node_modules/**", "enterprise/**")
}
androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar(Char::uppercaseChar)
        val verify = tasks.register<VerifyPersianTranslationsTask>("verify${capitalized}PersianTranslations") {
            group = "verification"
            description = "Verify Persian overlays against defaults in this variant's project dependencies."
            defaultResources.from(defaultTranslationResources)
            overrides.set(layout.projectDirectory.file("src/main/res/values-fa/persian_overrides.xml"))
            script.set(rootProject.layout.projectDirectory.file("tools/localization/verify-persian.py"))
            repositoryDirectory.set(rootProject.layout.projectDirectory)
            // Public Gradle/AGP APIs; resolve only when this task's inputs are queried.
            dependencyProjects.set(variant.runtimeConfiguration.incoming.resolutionResult.rootComponent.map(::resolvedProjectPaths))
            receipt.set(layout.buildDirectory.file("localizationVerification/${variant.name}.json"))
        }
        val guardedTasks = setOf("lint$capitalized", "lintVital$capitalized", "assemble$capitalized", "bundle$capitalized")
        tasks.matching { it.name in guardedTasks }.configureEach { dependsOn(verify) }
    }
}
