import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTargetVersion: String by project
val jacksonVersion: String by project
val avroVersion: String by project
val semverVersion: String by project
val jsonSchemaValidatorVersion: String by project
val junitPlatformRunnerVersion: String by project
val junitVersion: String by project
val openapiDiffVersion: String by project
val kafkaSchemaRegistryClientVersion: String by project

buildscript {
    val dinterPluginVersion: String by project

    apply(from = "../gradle/artifactory.gradle")
    repositories {
        maven { url = uri("https://repo.spring.io/milestone") }
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

tasks.withType<PublishToMavenRepository> {
    dependsOn("customizePublishing")
}

plugins {
    // TODO - use  property
    kotlin("jvm") version "1.9.21"
}


repositories {
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://packages.confluent.io/maven/") }
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("com.github.zafarkhaja:java-semver:$semverVersion")
    implementation("com.github.java-json-tools:json-schema-validator:$jsonSchemaValidatorVersion")
    implementation("org.openapitools.openapidiff:openapi-diff-core:$openapiDiffVersion")
    implementation("io.confluent:kafka-schema-registry-client:$kafkaSchemaRegistryClientVersion")

    testImplementation("org.junit.platform:junit-platform-runner:$junitPlatformRunnerVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmTargetVersion
    }
}
