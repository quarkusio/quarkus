package io.quarkus.kotlin.serialization

import io.quarkus.runtime.annotations.ConfigGroup
import io.quarkus.runtime.annotations.ConfigItem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNames

@ConfigGroup
open class JsonConfiguration {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    @ConfigItem(defaultValue = "true")
    @JvmField
    var encodeDefaults: Boolean = true
    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var ignoreUnknownKeys: Boolean = false
    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var isLenient: Boolean = false
    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var allowStructuredMapKeys: Boolean = false
    /**
     * Specifies whether resulting JSON should be pretty-printed.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var prettyPrint: Boolean = false
    /**
     * Specifies indent string to use with [prettyPrint] mode
     */
    @ConfigItem(defaultValue = "    ")
    @JvmField
    var prettyPrintIndent: String = "    "
    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     *   1. JSON value is `null` but property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var coerceInputValues: Boolean = false
    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var useArrayPolymorphism: Boolean = false
    /**
     * Name of the class descriptor property for polymorphic serialization.
     */
    @ConfigItem(defaultValue = "type")
    @JvmField
    var classDiscriminator: String = "type"
    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     */
    @ConfigItem(defaultValue = "false")
    @JvmField
    var allowSpecialFloatingPointValues: Boolean = false
    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     */
    @ConfigItem(defaultValue = "true")
    @JvmField
    var useAlternativeNames: Boolean = true

    override fun toString(): String {
        return "JsonConfiguration(encodeDefaults=$encodeDefaults, ignoreUnknownKeys=$ignoreUnknownKeys, isLenient=$isLenient, allowStructuredMapKeys=$allowStructuredMapKeys, prettyPrint=$prettyPrint, prettyPrintIndent='$prettyPrintIndent', coerceInputValues=$coerceInputValues, useArrayPolymorphism=$useArrayPolymorphism, classDiscriminator='$classDiscriminator', allowSpecialFloatingPointValues=$allowSpecialFloatingPointValues)"
    }
}