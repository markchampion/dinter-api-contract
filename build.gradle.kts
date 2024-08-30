import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.gradle.api.publish.maven.MavenPublication
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask
import java.util.Locale

plugins {
    id("java")
    `maven-publish`
    id("org.openapi.generator") version "7.2.0"
    id("com.github.davidmc24.gradle.plugin.avro-base") version "1.9.1"
}

group = "com.dinter"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val taskNames = mutableListOf<String>()
val specFiles = fileTree("$rootDir/src/main/resources/api")
    .matching {
        include("**/**/*.yaml")
        exclude("**/schema/*.yaml")
    }

specFiles.forEach { specFile ->
    val taskName = "openApiValidate" + specFile.nameWithoutExtension.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    tasks.register(taskName, ValidateTask::class) {
        inputSpec.set(specFile.absolutePath)
    }
    taskNames.add(taskName)
}
tasks.register("openApiValidateAll") { dependsOn(taskNames) }

val avroFiles = fileTree("$rootDir/src/main/resources/kafka")
    .matching {
        include("**/**/*.avsc")
    }

tasks.register("validateAvroSchemas") {
    doLast {
        avroFiles.forEach { file ->
            try {
                Schema.Parser().parse(file)
                println("Schema ${file.name} is valid.")
            } catch (e: SchemaParseException) {
                println("Schema ${file.name} is invalid: ${e.message}")
                throw e
            }
        }
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.build {
    dependsOn("openApiValidateAll")
}

dependencies {
    implementation ("org.apache.avro:avro:1.11.3")
    implementation("org.apache.avro:avro-tools:1.11.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}