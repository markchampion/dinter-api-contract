package com.dinter.plugin.contracts.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.main.JsonSchemaFactory
import java.io.File
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException

class AvroSchema(
    override val context: String,
    override val name: String,
    override val file: File,
    override val majorVersion: Int
) : Contract {
    override val type = Contract.ContractType.AVRO

    override fun getInstance(fileReader: Contract.FileReader) =
        fileReader.readToString(file)?.let { Instance(it, this) }

    class AvroSchemaCreator: Contract.ContractCreator<AvroSchema> {
        override fun construct(contractFile: File, match: MatchResult) = AvroSchema(
            file = contractFile,
            context = match.groups[1]!!.value,
            name = match.groups[2]!!.value,
            majorVersion = match.groups[3]!!.value.toInt()
        )
    }

    class Instance(override val contents: String, override val contract: AvroSchema) : Contract.Instance {

        val node = objectMapper.readTree(contents)

        val schema = try {
            Schema.Parser().parse(contents)
        } catch (e: SchemaParseException) {
            raiseValidationError("Could not parse schema: ${e.message}", e)
        }

        override val version: String = node.get("x-dinter-metadata")?.get("version")?.asText()
            ?: raiseValidationError("Avro schema must have version specified in x-dinter-metadata")

        val messageType: MessageType =
            node.get("x-dinter-metadata")?.get("type")?.asText()?.let { typename ->
                MessageType.entries.firstOrNull { it.name == typename }
                    ?: raiseValidationError("Invalid message type: $typename. Must be one of ${MessageType.entries}")
            } ?: raiseValidationError("Avro schema must specify x-dinter-metadata.type as one of ${MessageType.entries}")

        val topic: String = node.get("x-dinter-metadata")?.get("topic")?.asText()
            ?: raiseValidationError("Avro schema must specify x-dinter-metadata.topic")

        override fun validate() {
            messageType

            validateJsonSchema()

            if (semver.majorVersion().toInt() != contract.majorVersion) {
                raiseValidationError("Filename ${contract.file.path} does not match the major version specified in schema: $semver")
            }

            val expectedNamespace =
                "com.dinter.${contract.context.replace("-", "")}.avro.contract." +
                        "${contract.name.replace("-", "")}.v${semver.majorVersion()}"
            if (schema.namespace != expectedNamespace) {
                raiseValidationError("Namespace is expected to be: \"$expectedNamespace\"")
            }

            val expectedTopic = "${contract.context}.${contract.name}.v${contract.majorVersion}"
            if (expectedTopic != topic) {
                raiseValidationError("x-dinter-metadata.topic value is expected to be: \"$expectedTopic\"")
            }
        }

        private fun validateJsonSchema() {
            val schemaValidationReport = jsonSchema.validate(node)
            if (!schemaValidationReport.isSuccess) {
                raiseValidationError("Schema does not conform to JSON schema validation: $schemaValidationReport")
            }
        }
    }

    enum class MessageType {
        SNAPSHOT,
        EVENT,
        COMMAND;
    }

    companion object {
        val objectMapper = ObjectMapper()
        val jsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(
            "resource:/avro-schema.json"
        )!!
    }
}
