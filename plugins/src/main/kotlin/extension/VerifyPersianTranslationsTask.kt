/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package extension

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

fun androidResourceSourceSets(flavors: List<String>, buildType: String?, variantName: String): List<String> {
    val combinedFlavor = flavors.firstOrNull().orEmpty() + flavors.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    return (listOf("main") + flavors.asReversed() + listOfNotNull(combinedFlavor.takeIf(String::isNotEmpty), buildType, variantName))
        .distinct()
}

// Walk the selected variants, including Android matchingFallbacks, rather than using the app's build type for every library.
fun resolvedLibraryResourceLayers(root: ResolvedComponentResult): List<String> {
    val pending = ArrayDeque(root.dependencies.filterIsInstance<ResolvedDependencyResult>())
    val visited = mutableSetOf<ResolvedVariantResult>()
    val layers = mutableListOf<String>()
    while (pending.isNotEmpty()) {
        val dependency = pending.removeFirst()
        val variant = dependency.resolvedVariant
        if (!visited.add(variant)) continue
        pending.addAll(dependency.selected.getDependenciesForVariant(variant).filterIsInstance<ResolvedDependencyResult>())
        val owner = (dependency.selected.id as? ProjectComponentIdentifier)?.projectPath ?: continue
        val attributes = variant.attributes
        val buildType = attributes.getAttribute(BuildTypeAttr.ATTRIBUTE)?.name ?: continue
        val flavors = attributes.keySet().filter { it.name.startsWith("com.android.build.api.attributes.ProductFlavor:") }.mapNotNull {
            val dimension = it.name.substringAfter(':')
            attributes.getAttribute(ProductFlavorAttr.of(dimension))?.name
        }
        val variantName = variant.displayName.removeSuffix("RuntimeElements")
        val projectDirectory = owner.removePrefix(":").replace(':', '/')
        androidResourceSourceSets(flavors, buildType, variantName).forEachIndexed { priority, sourceSet ->
            layers.add("$owner\t$priority\t$projectDirectory/src/$sourceSet/res")
        }
    }
    return layers
}

@CacheableTask
abstract class VerifyPersianTranslationsTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val overrides: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val script: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val exclusions: RegularFileProperty

    @get:Input
    abstract val resourceLayers: ListProperty<String>

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        val files = temporaryDir.resolve("resource-files.txt")
        files.writeText(resources.files.sortedBy { it.path }.joinToString("\n") { it.absolutePath })
        val layers = temporaryDir.resolve("resource-layers.tsv")
        layers.writeText(resourceLayers.get().joinToString("\n"))
        exec.exec {
            commandLine(
                "python3", script.get().asFile, repositoryDirectory.get().asFile, overrides.get().asFile,
                "--file-list", files, "--layers", layers, "--exclusions", exclusions.get().asFile, "--receipt", receipt.get().asFile,
            )
        }
    }
}
