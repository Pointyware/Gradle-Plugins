package org.pointyware.gradle.buildconfig

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File
import java.util.Properties

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
