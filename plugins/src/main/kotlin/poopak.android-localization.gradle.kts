/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import extension.VerifyPersianTranslationsTask
import extension.androidResourceSourceSets
import extension.resolvedLibraryResourceLayers

plugins {
    id("com.android.application")
}

val libraryResources = rootProject.fileTree(rootProject.projectDir) {
    include("**/src/*/res/values/*.xml", "**/src/*/res/values-fa/*.xml", "**/src/*/res/values-b+fa/*.xml")
    exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/node_modules/**")
}
val localizationRoot = rootProject.layout.projectDirectory
val applicationPath = project.path
androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar(Char::uppercaseChar)
        val sourceSets = androidResourceSourceSets(variant.productFlavors.map { it.second }, variant.buildType, variant.name)
        val appSources = sourceSets.map { name -> android.sourceSets.findByName(name)?.res?.directories.orEmpty() }
        val appLayers = appSources.flatMapIndexed { priority, directories ->
            directories.map { directory ->
                val relative = file(directory).relativeTo(localizationRoot.asFile).invariantSeparatorsPath
                "$applicationPath\t$priority\t$relative"
            }
        }
        val libraryLayers = variant.runtimeConfiguration.incoming.resolutionResult.rootComponent.map(::resolvedLibraryResourceLayers)
        val verify = tasks.register<VerifyPersianTranslationsTask>("verify${capitalized}PersianTranslations") {
            group = "verification"
            description = "Verify Persian coverage and format arguments for this variant's project resources."
            resources.from(libraryResources, appSources.flatten().map { directory ->
                fileTree(directory) { include("values/*.xml", "values-fa/*.xml", "values-b+fa/*.xml") }
            })
            overrides.set(layout.projectDirectory.file("src/main/res/values-fa/persian_overrides.xml"))
            script.set(localizationRoot.file("tools/localization/verify-persian.py"))
            exclusions.set(localizationRoot.file("tools/localization/persian-exclusions.json"))
            repositoryDirectory.set(localizationRoot)
            resourceLayers.set(libraryLayers.map { it + appLayers })
            receipt.set(layout.buildDirectory.file("localizationVerification/${variant.name}.json"))
        }
        val guardedTasks = setOf("lint$capitalized", "lintVital$capitalized", "assemble$capitalized", "bundle$capitalized")
        tasks.matching { it.name in guardedTasks }.configureEach { dependsOn(verify) }
    }
}
