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

                scm {
                    connection = "scm:git:github.com/Pointyware/Gradle-Plugins.git"
                    developerConnection = "scm:git:github.com/Pointyware/Gradle-Plugins.git"
                }

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                    }
                }

                organization {
                    name = "Pointyware"
                    url = "https://pointyware.org"
                }

                developers {
                    developer {
                        name = "Pointyware Team"
                        url = "https://github.com/Pointyware"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "MavenCentral"
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
                password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
            }
        }
    }
}
