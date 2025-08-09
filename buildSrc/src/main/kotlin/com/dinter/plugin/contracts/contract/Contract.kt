package com.dinter.plugin.contracts.contract

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import org.gradle.api.GradleException
import java.io.File

interface Contract {
    val context: String
    val type: ContractType
    val name: String
    val file: File
    val majorVersion: Int
    fun getInstance(fileReader: FileReader): Instance?

    interface Instance {
        val contract: Contract
        val contents: String
        val version: String
        fun validate()

        val semver: Version
        get() = try {
            Version.parse(version)
        } catch (e: ParseException) {
            raiseValidationError("Not a valid semantic version: $version", e)
        }

        fun raiseValidationError(message: String, cause: Exception? = null): Nothing {
            throw GradleException("File ${contract.file.path}: $message", cause)
        }
    }

    interface FileReader {
        fun readToString(relativePath: File): String?
    }

    interface ContractCreator<C: Contract> {
        fun construct(contractFile: File, match: MatchResult) : C
    }

    enum class ContractType(
        val pattern: Regex,
        val creator: ContractCreator<*>
    ) {
        AVRO(
            pattern =Regex(
                "^contracts/(?<context>[a-z][a-z0-9-]*)/avro/(?<name>[a-z][a-z0-9-]*)/v(?<majorVersion>\\d+).avsc$"
            ),
            creator = AvroSchema.AvroSchemaCreator()
        ),
        OPENAPI(
            pattern = Regex("^contracts/(?<context>[a-z][a-z0-9_-]*)/openapi/(?<name>[a-z][a-z0-9]*)/v(?<majorVersion>\\d+).yaml$"),
            creator = OpenApiSchema.OpenApiSchemaCreator()
        );
    }
}
