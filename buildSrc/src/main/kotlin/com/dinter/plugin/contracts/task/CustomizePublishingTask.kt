package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.Contract
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskAction

open class CustomizePublishingTask  : ContractTask<Contract>() {

    @TaskAction
    fun action() {
        project.plugins.apply(PublishingPlugin::class.java)
        project.extensions.configure(PublishingExtension::class.java) {
            it.publications { pc ->
                changedContracts.forEach { contract ->
                    val contractInstance = contract.getInstance(localFileReader)!!
                    pc.create(
                        "${contract.context}${contract.name}${contract.majorVersion}",
                        MavenPublication::class.java
                    ) { mp ->
                        mp.groupId = "com.dinter.contracts.${contract.type.name.lowercase()}.${contract.context}"
                        mp.artifactId = contract.name
                        mp.version = contractVersion(contract.majorVersion, contractInstance.semver.toString())
                        println("Publishing contract: ${mp.groupId}:${mp.artifactId}:${mp.version}")
                        mp.artifact(contract.file)
                    }
                }
            }
        }
    }

    private fun contractVersion(majorVersion: Int, version: String): String {
        return if ("true" == System.getProperty("release-non-snapshot")) {
            version
        } else {
            val branchPart = getCurrentBranch().replace(Regex(".*/"), "").uppercase().ifEmpty { "unknown" }
            "v$majorVersion-$branchPart-SNAPSHOT"
        }
    }
}


