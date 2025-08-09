package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.Contract
import org.gradle.api.tasks.TaskAction

open class ValidateContractsTask : ContractTask<Contract>() {

    @TaskAction
    fun action() {
        changedOrAllContracts.forEach { contract ->
            project.logger.warn("Validating ${contract.file.path}")
            contract.getInstance(localFileReader)!!.validate()
        }
    }
}
