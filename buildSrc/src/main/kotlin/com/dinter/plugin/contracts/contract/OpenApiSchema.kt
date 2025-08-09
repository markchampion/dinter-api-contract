package com.dinter.plugin.contracts.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class OpenApiSchema(
    override val context: String,
    override val name: String,
    override val file: File,
    override val majorVersion: Int
) : Contract {
    override val type = Contract.ContractType.OPENAPI

    override fun getInstance(fileReader: Contract.FileReader) =
        fileReader.readToString(file)?.let { Instance(it, this) }

    class OpenApiSchemaCreator: Contract.ContractCreator<OpenApiSchema> {
        override fun construct(contractFile: File, match: MatchResult) = OpenApiSchema(
            file = contractFile,
            context = match.groups[1]!!.value,
            name = match.groups[2]!!.value,
            majorVersion = match.groups[3]!!.value.toInt()
        )
    }

    class Instance(override val contents: String, override val contract: OpenApiSchema) : Contract.Instance {

        override fun validate() {

            if (semver.majorVersion().toInt() != contract.majorVersion) {
                raiseValidationError("Filename does not match the major version specified in schema: $semver")
            }
        }

        override val version: String =
            objectMapper.readTree(contents).get("info")?.get("version")?.asText()
                ?: raiseValidationError("Schema must specify info.version")
    }

    companion object {
        val objectMapper = ObjectMapper(YAMLFactory())
    }
}
