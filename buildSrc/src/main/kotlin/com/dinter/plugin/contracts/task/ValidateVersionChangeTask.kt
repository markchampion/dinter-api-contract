package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.Contract
import org.gradle.api.tasks.TaskAction

open class ValidateVersionChangeTask : ContractTask<Contract>() {

    @TaskAction
    fun action() {
        if (changedContracts.isEmpty()) {
            project.logger.warn("No changed contracts")
        }
        changedContracts.forEach { contract ->
            val previousInstance = contract.getInstance(gitFileReader(compareCommitRef))
            val newInstance = contract.getInstance(localFileReader)!!
            project.logger.warn("Validating version change for ${contract.file.path}: ${previousInstance?.version} -> ${newInstance.version}")
            validateVersionChange(previousInstance, newInstance)
        }
    }

    private fun validateVersionChange(previousInstance: Contract.Instance?, newInstance: Contract.Instance) {
        if (previousInstance == null) return
        val previousVersion = previousInstance.semver
        val newVersion= newInstance.semver
        if (newVersion !in listOf(
                previousVersion.nextPatchVersion(), previousVersion.nextMinorVersion()
            )
        ) {
            newInstance.raiseValidationError(
                "The version change $previousVersion -> $newVersion " + "is not correct. " +
                        "You need to increase minor or patch version by one."
            )
        }
    }
}
