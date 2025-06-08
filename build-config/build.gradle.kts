/*
 * Copyright (c) 2024 Pointyware. Use of this software is governed by the GPL-3.0 license.
 */

plugins {
    `kotlin-dsl`
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    testImplementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("build-config") {
            id = "org.pointyware.cymatics.build-config"
            implementationClass = "org.pointyware.build.BuildConfigPlugin"
        }
    }
}

group = "org.pointyware.gradle.buildconfig"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = "build-config-gradle-plugin"
                description = "A Gradle plugin for generating BuildConfig files across multiple platforms."
                url = "https://github.com/Pointyware/Gradle-Plugins"
            }
        }
    }
}
