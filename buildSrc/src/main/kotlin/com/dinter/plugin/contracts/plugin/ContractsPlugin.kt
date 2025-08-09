package com.dinter.plugin.contracts.plugin

import com.dinter.plugin.contracts.task.CustomizePublishingTask
import com.dinter.plugin.contracts.task.ValidateCompatibilityTask
import com.dinter.plugin.contracts.task.ValidateContractsTask
import com.dinter.plugin.contracts.task.ValidateNoContractsInWrongDirTask
import com.dinter.plugin.contracts.task.ValidateVersionChangeTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ContractsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.apply {
            val customizePublishing = create("customizePublishing", CustomizePublishingTask::class.java)
            customizePublishing.action()
        }

        target.tasks.apply {
            val validateVersionChangeTask = create("validateVersionChange", ValidateVersionChangeTask::class.java)
            val validateContractsTask = create("validateContracts", ValidateContractsTask::class.java)
            val validateNoContractsInWrongDirTask = create("ValidateNoContractsInWrongDir", ValidateNoContractsInWrongDirTask::class.java)
            val validateCompatibilityTask = create("validateCompatibility", ValidateCompatibilityTask::class.java)

            create("checkContractChanges") { checkContractChanges ->
                checkContractChanges.group = "contracts"
                checkContractChanges.dependsOn(validateVersionChangeTask)
                checkContractChanges.dependsOn(validateContractsTask)
                checkContractChanges.dependsOn(validateNoContractsInWrongDirTask)
                checkContractChanges.dependsOn(validateCompatibilityTask)
            }
        }
    }
}
