package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface JsonConfig {
    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     */
    @WithDefault("false")
    boolean allowSpecialFloatingPointValues();

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     */
    @WithDefault("false")
    boolean allowStructuredMapKeys();

    /**
     * Name of the class descriptor property for polymorphic serialization.
     */
    @WithDefault("type")
    String classDiscriminator();

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     * 1. JSON value is `null` but property type is non-nullable.
     * 2. Property type is an enum type, but JSON value contains unknown enum member.
     */
    @WithDefault("false")
    boolean coerceInputValues();

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    @WithDefault("true")
    boolean encodeDefaults();

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     * <p>
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     * <p>
     * {@code true} by default.
     */
    @WithDefault("true")
    boolean explicitNulls();

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     */
    @WithDefault("false")
    boolean ignoreUnknownKeys();

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     * <p>
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     */
    @WithDefault("false")
    boolean isLenient();

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     */
    @WithDefault("false")
    boolean prettyPrint();

    /**
     * Specifies indent string to use with [prettyPrint] mode
     */
    @WithDefault("    ")
    String prettyPrintIndent();

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     * <p>
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     */
    @WithDefault("true")
    boolean useAlternativeNames();

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     */
    @WithDefault("false")
    boolean useArrayPolymorphism();

    /**
     * Specifies the {@code JsonNamingStrategy} that should be used for all properties in classes for serialization and
     * deserialization.
     * This strategy is applied for all entities that have {@code StructureKind.CLASS}.
     * <p>
     * <p>
     * {@code null} by default.
     * <p>
     * <p>
     * This element can be one of two things:
     * <ol>
     * <li>the fully qualified class name of a type implements the {@code NamingStrategy} interface and has a no-arg
     * constructor</li>
     * <li>a value in the form {@code NamingStrategy.SnakeCase} which refers to built-in values provided by the kotlin
     * serialization
     * library itself.
     * </li>
     * </ol>
     */
    Optional<String> namingStrategy();

    /**
     * Specifies if the enum values should be decoded case insensitively.
     */
    @WithDefault("false")
    boolean decodeEnumsCaseInsensitive();

    /**
     * Specifies if trailing comma is allowed.
     */
    @WithDefault("false")
    boolean allowTrailingComma();

    /**
     * Allows parser to accept C/Java-style comments in JSON input.
     */
    @WithDefault("false")
    boolean allowComments();

}
