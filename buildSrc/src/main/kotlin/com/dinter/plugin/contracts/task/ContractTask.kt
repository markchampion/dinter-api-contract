package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.Contract
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import java.io.File
import java.nio.file.Files

@Suppress("UNCHECKED_CAST")
open class ContractTask<C : Contract> : DefaultTask() {

    private fun arePluginClassesChanged() =
        getChangedFiles().any { file -> file.unixPath.startsWith("buildSrc/") }

    @get:Internal
    val changedContracts: List<C> by lazy  {
        getChangedFiles().mapNotNull { contractFromFile(it) }
    }

    @get:Internal
    val allContracts: List<C>
        get() = Files.walk(project.projectDir.resolve("contracts").toPath()).filter { Files.isRegularFile(it) }.toList().mapNotNull { path ->
            contractFromFile(path.toFile().relativeTo(project.projectDir))
        }

    @get:Internal
    val changedOrAllContracts: List<C>
        get() = if (arePluginClassesChanged()) allContracts else changedContracts

    @Internal
    val localFileReader = object : Contract.FileReader {
        override fun readToString(relativePath: File): String? {
            val file = project.projectDir.resolve(relativePath)
            if (!file.exists())
                return null
            return file.readText()
        }
    }

    @Internal
    fun gitFileReader(gitRef: String) = object : Contract.FileReader {
        override fun readToString(relativePath: File): String? {
            val process = runCommandInProjectDir("git", "show", "$gitRef:${relativePath.unixPath}")
            val text = process.inputStream.bufferedReader().readText()
            if (process.errorStream.bufferedReader().readText().isNotEmpty()) return null
            return text
        }
    }

    protected fun contractFromFile(file: File): C? {
        Contract.ContractType.entries.forEach {
            it.pattern.matchEntire(file.unixPath)?.let { match ->
                return it.creator.construct(file, match) as C
            }
        }
        return null
    }

    private fun getChangedFiles(): List<File> {
        project.logger.warn("Getting changed files for ref $compareCommitRef")
        val proc = runCommandInProjectDir("git", "diff", "--name-only", compareCommitRef)
        return proc.inputStream.bufferedReader().lineSequence().map { File(it) }.toList()
    }

    @get:Internal
    protected val compareCommitRef by lazy {
        val currentBranch = getCurrentBranch()
        project.logger.warn("Current branch \"$currentBranch\"")
        if ("true" == System.getProperty("compare-with-main")) {
            "HEAD~1"
        } else {
            runCommandInProjectDir("git", "merge-base", "origin/main", "HEAD")
                .inputStream.bufferedReader().readLine().orEmpty().ifBlank { "HEAD" }
        }
    }

    @Internal
    protected fun getCurrentBranch() = runCommandInProjectDir("git", "rev-parse", "--abbrev-ref", "HEAD")
        .inputStream.bufferedReader().readText().trim()

    private fun runCommandInProjectDir(vararg command: String) = ProcessBuilder(command.asList()).apply {
        directory(project.projectDir)
        redirectOutput(ProcessBuilder.Redirect.PIPE)
        redirectError(ProcessBuilder.Redirect.PIPE)
    }.start()!!
}

val File.unixPath
    get() = this.toPath().joinToString(separator = "/")