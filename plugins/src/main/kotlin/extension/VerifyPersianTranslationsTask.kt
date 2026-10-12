/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package extension

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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

fun resolvedProjectPaths(root: ResolvedComponentResult): List<String> {
    val pending = ArrayDeque<ResolvedComponentResult>()
    val visited = mutableSetOf<ComponentIdentifier>()
    val paths = mutableSetOf<String>()
    pending.add(root)
    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!visited.add(component.id)) continue
        (component.id as? ProjectComponentIdentifier)?.let { paths.add(it.projectPath) }
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { pending.add(it.selected) }
    }
    return paths.sorted()
}

@CacheableTask
abstract class VerifyPersianTranslationsTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val defaultResources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val overrides: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val script: RegularFileProperty

    @get:Input
    abstract val dependencyProjects: ListProperty<String>

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        val files = temporaryDir.resolve("default-files.txt")
        files.writeText(defaultResources.files.sortedBy { it.path }.joinToString("\n") { it.absolutePath })
        val projects = temporaryDir.resolve("dependency-projects.txt")
        projects.writeText(dependencyProjects.get().joinToString("\n"))
        exec.exec {
            commandLine(
                "python3", script.get().asFile, repositoryDirectory.get().asFile, overrides.get().asFile,
                "--file-list", files, "--projects", projects, "--receipt", receipt.get().asFile,
            )
        }
    }
}
