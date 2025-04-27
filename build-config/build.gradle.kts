/*
 * Copyright (c) 2024 Pointyware. Use of this software is governed by the GPL-3.0 license.
 */

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("build-config") {
            id = "org.pointyware.cymatics.build-config"
            implementationClass = "org.pointyware.cymatics.buildlogic.BuildConfigPlugin"
        }
    }
}
