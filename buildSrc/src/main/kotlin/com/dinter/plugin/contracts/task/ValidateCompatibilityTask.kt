package com.dinter.plugin.contracts.task

import com.dinter.plugin.contracts.contract.AvroSchema
import com.dinter.plugin.contracts.contract.Contract
import io.confluent.kafka.schemaregistry.CompatibilityChecker
import io.confluent.kafka.schemaregistry.CompatibilityLevel
import org.gradle.api.tasks.TaskAction
import org.openapitools.openapidiff.core.OpenApiCompare
import org.openapitools.openapidiff.core.output.ConsoleRender
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

open class ValidateCompatibilityTask : ContractTask<Contract>() {

    @TaskAction
    fun action() {
        if (changedContracts.isEmpty()) {
            project.logger.warn("No changed contracts")
        }
        changedContracts.forEach { contract ->
            val previousInstance = contract.getInstance(gitFileReader(compareCommitRef))
            val newInstance = contract.getInstance(localFileReader)!!
            project.logger.warn("Validating schema compatibility for ${contract.file.path}: ${previousInstance?.version} -> ${newInstance.version}")
            validateVersionCompatibility(previousInstance, newInstance)
        }
    }

    private fun validateVersionCompatibility(previousInstance: Contract.Instance?, newInstance: Contract.Instance) {
        if (previousInstance == null) return
        when (newInstance.contract.type) {
            Contract.ContractType.AVRO -> validateAvroCompatibility(
                previousInstance as AvroSchema.Instance,
                newInstance as AvroSchema.Instance
            )
            Contract.ContractType.OPENAPI -> validateOpenApiCompatibility(previousInstance, newInstance)
        }
    }
    private fun validateOpenApiCompatibility(previousInstance: Contract.Instance, newInstance: Contract.Instance) {
        val diff = OpenApiCompare.fromContents(previousInstance.contents, newInstance.contents)
        if (diff.isIncompatible) {
            ByteArrayOutputStream().use { outputStream ->
                OutputStreamWriter(outputStream).use { outputStreamWriter ->
                    ConsoleRender().render(diff, outputStreamWriter)
                    newInstance.raiseValidationError("Breaking changes detected ${previousInstance.semver} -> ${newInstance.semver}!\n${outputStream}")
                }
            }
        }
    }
    private fun validateAvroCompatibility(previousInstance: AvroSchema.Instance, newInstance: AvroSchema.Instance) {
        if(previousInstance.messageType != newInstance.messageType) newInstance.raiseValidationError(
            "messageType cannot be changed: ${previousInstance.messageType}->${newInstance.messageType}")
        val compatibilityLevel = getAvroCompatibilityLevel(newInstance)
        project.logger.debug("Compatibility level for {} = {}", newInstance.schema.fullName, compatibilityLevel)
        val checker = CompatibilityChecker.checker(compatibilityLevel)
        val result = checker.isCompatible(
            io.confluent.kafka.schemaregistry.avro.AvroSchema(newInstance.contents),
            listOf(io.confluent.kafka.schemaregistry.avro.AvroSchema(previousInstance.contents))
        )
        if (result.isNotEmpty()) {
            newInstance.raiseValidationError(createAvroValidationExceptionMessage(result, compatibilityLevel))
        }
    }

    private fun getAvroCompatibilityLevel(
        instance: AvroSchema.Instance
    ): CompatibilityLevel = when (instance.messageType) {
        AvroSchema.MessageType.SNAPSHOT -> CompatibilityLevel.FULL
        AvroSchema.MessageType.EVENT -> CompatibilityLevel.FORWARD
        AvroSchema.MessageType.COMMAND -> CompatibilityLevel.BACKWARD
    }

    private fun createAvroValidationExceptionMessage(
        validationErrors: List<String>,
        compatibilityLevel: CompatibilityLevel
    ) = buildString {
        append("Avro compatibility (level: $compatibilityLevel) failed with: ")
        if (validationErrors.size == 1) {
            append(validationErrors.first())
        } else {
            validationErrors.forEach {
                append("\n - $it")
            }
        }

    }


}
