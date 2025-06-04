
/**
 * Generates a BuildConfig kotlin file for the project that defines all the configured values.
 */
class BuildConfigPlugin: Plugin<Project> {

    private fun loadFileIntoProperties(file: File, properties: Properties, required: Boolean = false) {
        if (file.exists()) {
            file.inputStream().use {
                properties.load(it)
            }
        } else {
            logger.log(
                if (required) LogLevel.ERROR else LogLevel.WARN,
                "Properties file ${file.name} does not exist."
            )
        }
    }

    override fun apply(target: Project) {
        val buildConfigFile = File(
            target.layout.buildDirectory.asFile.get(),
            "generated/source/buildconfig/BuildConfig.kt"
        )
        val extension = target.extensions.create("buildConfig", BuildConfigPluginExtension::class.java)
        extension.packageName.convention("${target.group}")
        extension.defaultPropertiesFileName.convention("")
        extension.overridingPropertiesFileName.convention("")
        extension.properties.convention(mapOf())

        target.tasks.register("generateBuildConfig") {
            group = "build"
            description = "Generates a BuildConfig file with the configured properties."

            val properties = Properties()
            extension.defaultPropertiesFileName.get().takeIf { it.isNotBlank() }?.let { default ->
                loadFileIntoProperties(target.file(default), properties, required = true)
            }
            extension.overridingPropertiesFileName.get().takeIf { it.isNotBlank() }?.let { override ->
                loadFileIntoProperties(target.file(override), properties)
            }
            extension.properties.get().forEach {
                properties.setProperty(it.key, it.value)
            }

            val packageName = extension.packageName.get()

            val propertiesString = properties.map { (key, value) ->
                "    const val $key = \"$value\""
            }.joinToString("\n")
            doLast {
                buildConfigFile.parentFile.mkdirs()
                buildConfigFile.writeText(
                    """package $packageName

object BuildConfig {
$propertiesString
}
"""
                )
            }
        }

        target.tasks.withType(KotlinCompile::class.java) {
            dependsOn("generateBuildConfig")

            source(buildConfigFile)
        }
        when {
            // Support Kotlin Multiplatform projects
            target.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                logger.log(LogLevel.INFO, "Configuring Kotlin Multiplatform extension")
                target.extensions.configure(KotlinMultiplatformExtension::class.java) {
                    sourceSets.named("commonMain") {
                        kotlin.srcDirs(buildConfigFile.parentFile)
                    }
                }
            }
            // Support Kotlin JVM projects
            target.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> {
                logger.log(LogLevel.INFO, "Configuring Kotlin JVM extension")
                target.extensions.configure(KotlinJvmProjectExtension::class.java) {
                    sourceSets.named("main") {
                        kotlin.srcDirs(buildConfigFile.parentFile)
                    }
                }
            }
            else -> {
                logger.log(LogLevel.ERROR, "Unsupported project type. Please apply the Kotlin Multiplatform or Kotlin JVM plugin.")
            }
        }
    }
}

/**
 * Must be called in the build script where the plugin is applied.
 *
 * Based on Android's BuildConfig setup.
 * ```kotlin
 * buildConfig {
 *     packageName = "org.pointyware.xyz"
 *     defaultPropertiesFileName = "local.defaults.properties"
 *     overridingPropertiesFileName = "secrets.properties"
 *
 *     addString("API_PORT", System.getenv("API_PORT") ?: "8080")
 * }
 * ```
 *
 * To load properties selectively from a file, use the [BuildConfigPluginExtension.fromProperties]
 * method
 *
 * ```kotlin
 * buildConfig {
 *     fromProperties(file("secrets.properties")) {
 *         addString("API_KEY")
 *     }
 * }
 * ```
 *
 * The default and overriding properties files will be loaded first (if defined),
 * and then any
 *
 * ```kotlin
 * buildConfig {
 *     defaultPropertiesFileName = "local.defaults.properties"
 *     fromProperties(file("secrets.properties")) {
 *         if (project.hasProperty("release")) {
 *             addStringNamed("API_KEY", "API_KEY_RELEASE")
 *         } else {
 *             addStringNamed("API_KEY", "API_KEY_DEBUG")
 *         }
 *     }
 * }
 * ```
 */
fun Project.buildConfig(
    action: Action<BuildConfigPluginExtension>
) {
    extensions.configure(BuildConfigPluginExtension::class.java, action)
}

/**
 * Defines the available properties and methods for the BuildConfig plugin.
 */
abstract class BuildConfigPluginExtension {

    /**
     * The package name for the generated BuildConfig file.
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The name of the file to load with default properties. This file is meant
     * to be checked into version control and contains default values to allow
     * the project to build without any additional configuration, usually for
     * local development purposes.
     */
    @get:Input
    abstract val defaultPropertiesFileName: Property<String>

    /**
     * The name of the file to load with overriding properties. This file is
     * meant to be used for sensitive information that should not be checked
     * into version control, such as API keys or secrets. It allows
     * developers to override the default properties defined in [defaultPropertiesFileName].
     */
    @get:Input
    abstract val overridingPropertiesFileName: Property<String>

    @get:Input
    abstract val properties: MapProperty<String, String>

    fun addString(name: String, value: String) {
        properties.set(properties.get() + (name to value))
    }

    inner class FromPropertiesScope(
        private val properties: Properties
    ) {
        fun addString(key: String) {
            val value = properties.getProperty(key)
            if (value != null) {
                addString(key, value)
            } else {
                throw IllegalArgumentException("Key $key not found in properties file.")
            }
        }

        /**
         * Retrieves the property associated with [propertyName]
         * and retains it under the given [key] for
         * writing to the output BuildConfig.
         */
        fun addStringNamed(key: String, propertyName: String) {
            val value = properties.getProperty(propertyName)
            if (value != null) {
                addString(key, value)
            } else {
                throw IllegalArgumentException("Key $propertyName not found in properties file.")
            }
        }
    }

    /**
     * Loads properties from the given [file] and
     * executes the provided [block] in the context of
     * a [FromPropertiesScope] with the loaded properties.
     */
    fun fromProperties(file: File, block: FromPropertiesScope.() -> Unit) {
        if (!file.exists()) {
            throw IllegalArgumentException("File ${file.name} does not exist.")
        }
        val properties = Properties()
        file.inputStream().use {
            properties.load(it)
        }
        val scope = FromPropertiesScope(properties)
        try {
            scope.block()
        } catch (e: Exception) {
            throw IllegalArgumentException("Error loading properties from ${file.name}: ${e.message}", e)
        }
    }
}
