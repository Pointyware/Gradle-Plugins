/*
 * Copyright (c) 2024-2025 Pointyware. Use of this software is governed by the Affero GPL-3.0 license.
 */


plugins {
    `kotlin-dsl`
}

group = "org.pointyware.gradle.conventions"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmp-convention") {
            id = "org.pointyware.gradle.conventions.kmp"
            implementationClass = "org.pointyware.gradle.conventions.KotlinMultiplatformConventionPlugin"
        }
//        register("build-config") {
//            id = "org.pointyware.gradle.conventions.build-config"
//            implementationClass = "org.pointyware.gradle.conventions.BuildConfigPlugin"
//        }
    }
}
