import com.dinter.plugin.contracts.plugin.ContractsPlugin

apply(from = "./gradle/artifactory.gradle")
apply<ContractsPlugin>()

plugins {
    `maven-publish`
}

publishing {
    repositories {
        mavenLocal()
    }
}