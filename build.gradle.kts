plugins {
    `java-library`
    `maven-publish`

    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"

    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.fatumapp"

version = providers.gradleProperty("artifactVersion")
    .getOrElse("1.0.0-local")

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(
            "org.springframework.boot:spring-boot-dependencies:4.1.1"
        )
    }
}

dependencies {
    api("org.springframework:spring-web")

    implementation(
        "org.springframework.boot:spring-boot-autoconfigure"
    )
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
}
