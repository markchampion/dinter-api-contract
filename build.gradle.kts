import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.gradle.api.publish.maven.MavenPublication
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask
import java.util.Locale

plugins {
    id("java")
    `maven-publish`
    id("org.openapi.generator") version "7.2.0"
    id("com.jfrog.artifactory") version "5.+"
    id("com.github.davidmc24.gradle.plugin.avro-base") version "1.9.1"
}

group = "com.dinter.contract"
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

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components.getByName("java"))
            artifact(file("$rootDir/gradle.properties"))
        }
    }
}
configure<org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention> {
    clientConfig.isIncludeEnvVars = true

    setContextUrl(providers.gradleProperty("context_url").getOrNull())
    publish {
        repository {
            repoKey = providers.gradleProperty("repo_key").getOrNull()
            username = providers.gradleProperty("artifactory_user").getOrNull()
            password = providers.gradleProperty("artifactory_password").getOrNull()
        }

        defaults {
            publications("mavenJava")
            setPublishArtifacts(true)
            // Properties to be attached to the published artifacts.
            setProperties(mapOf(
                "qa.level" to "basic",
                "dev.team" to "core"
            ))
            setPublishPom(true) // Publish generated POM files to Artifactory (true by default)
            setPublishIvy(true) // Publish generated Ivy descriptor files to Artifactory (true by default)
        }
    }
}

tasks.build {
    dependsOn("openApiValidateAll")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}