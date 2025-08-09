package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.Contract
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

open class ValidateNoContractsInWrongDirTask : ContractTask<Contract>() {

    @TaskAction
    fun action() {
        val wrongFiles = Files.walk(project.projectDir.resolve("contracts").toPath()).filter { Files.isRegularFile(it) }.filter { path ->
            contractFromFile(path.toFile().relativeTo(project.projectDir)) == null
        }.toList()

        if (wrongFiles.isNotEmpty()) {
            throw GradleException("Files $wrongFiles are not in correct directory (do not match one of regexes ${Contract.ContractType.entries.map { it.pattern.toString() }}) ")
        }

    }
}