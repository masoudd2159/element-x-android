/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package extension

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

abstract class InstallBrandingToolsTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val toolingFiles: ConfigurableFileCollection

    @get:Input
    abstract val platform: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nodeExecutable: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val npmCli: RegularFileProperty

    @TaskAction
    fun install() {
        val version = ByteArrayOutputStream()
        exec.exec {
            commandLine(nodeExecutable.get().asFile, "--version")
            standardOutput = version
        }
        check(version.toString().trim() == "v24.12.0") { "Branding tooling requires Node 24.12.0 (see tools/branding/package.json)." }
        val target = outputDirectory.get().asFile.apply { mkdirs() }
        toolingFiles.forEach { it.copyTo(target.resolve(it.name), overwrite = true) }
        exec.exec {
            workingDir(target)
            commandLine(nodeExecutable.get().asFile, npmCli.get().asFile, "ci", "--ignore-scripts", "--no-audit", "--no-fund")
        }
    }
}

@CacheableTask
abstract class BrandIconsTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val embeddedCallAar: RegularFileProperty

    @get:OutputDirectory
    abstract val assetsDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artwork: ConfigurableFileCollection

    @get:Internal
    abstract val sourceDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val toolingFiles: ConfigurableFileCollection

    @get:Internal
    abstract val toolingDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nodeExecutable: RegularFileProperty

    @get:Input
    abstract val platform: Property<String>

    @get:Input
    abstract val brandId: Property<String>

    @get:Input
    abstract val background: Property<String>

    @get:Input
    abstract val extent: Property<Int>

    @get:Input
    abstract val logoFraction: Property<Double>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = sourceDirectory.get().asFile
        val callInput = temporaryDir.resolve("embedded-call.js")
        val callPath = ZipFile(embeddedCallAar.get().asFile).use { archive ->
            val entry = archive.entries().asSequence().filter {
                it.name.startsWith("assets/element-call/assets/index-") && it.name.endsWith(".js")
            }.singleOrNull() ?: error("Unsupported embedded call AAR layout; review its branding before building.")
            archive.getInputStream(entry).use { input -> callInput.outputStream().use { input.copyTo(it) } }
            entry.name.removePrefix("assets/")
        }
        exec.exec {
            commandLine(
                nodeExecutable.get().asFile,
                toolingDirectory.file("generate-icons.cjs").get().asFile,
                source,
                outputDirectory.get().asFile,
                background.get(),
                extent.get(),
                logoFraction.get(),
                assetsDirectory.get().asFile,
                callInput,
                callPath,
            )
        }
    }
}

@CacheableTask
abstract class VerifyBrandSourceTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val toolingFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nodeExecutable: RegularFileProperty

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:Internal
    abstract val toolingDirectory: DirectoryProperty

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        // The verifier scans exactly the declared inputs, including Kotlin and build scripts.
        val list = temporaryDir.resolve("source-files.txt")
        list.writeText(sourceFiles.files.sortedBy { it.path }.joinToString("\n") { it.absolutePath })
        exec.exec {
            commandLine(
                nodeExecutable.get().asFile,
                toolingDirectory.file("verify-source.cjs").get().asFile,
                repositoryDirectory.get().asFile,
                "--file-list",
                list,
            )
        }
        receipt.get().asFile.apply {
            parentFile.mkdirs()
            writeText("Brand source verification passed\n")
        }
    }
}

@CacheableTask
abstract class ValidateBrandArtworkTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artwork: ConfigurableFileCollection

    @get:Internal
    abstract val sourceDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val toolingFiles: ConfigurableFileCollection

    @get:Internal
    abstract val toolingDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nodeExecutable: RegularFileProperty

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun validate() {
        exec.exec {
            commandLine(nodeExecutable.get().asFile, toolingDirectory.file("generate-icons.cjs").get().asFile, "--validate", sourceDirectory.get().asFile)
        }
        receipt.get().asFile.apply {
            parentFile.mkdirs()
            writeText("Three canonical artwork files validated\n")
        }
    }
}
