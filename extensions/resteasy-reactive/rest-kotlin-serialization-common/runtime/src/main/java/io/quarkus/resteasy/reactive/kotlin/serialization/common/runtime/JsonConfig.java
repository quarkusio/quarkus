package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime;

import java.util.Optional;
import java.util.StringJoiner;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JsonConfig {
    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowSpecialFloatingPointValues = false;

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowStructuredMapKeys = false;

    /**
     * Name of the class descriptor property for polymorphic serialization.
     */
    @ConfigItem(defaultValue = "type")
    public String classDiscriminator = "type";

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     * 1. JSON value is `null` but property type is non-nullable.
     * 2. Property type is an enum type, but JSON value contains unknown enum member.
     */
    @ConfigItem(defaultValue = "false")
    public boolean coerceInputValues = false;

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    @ConfigItem(defaultValue = "true")
    public boolean encodeDefaults = true;

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     * <p>
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     * <p>
     * {@code true} by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean explicitNulls = true;

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     */
    @ConfigItem(defaultValue = "false")
    public boolean ignoreUnknownKeys = false;

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     * <p>
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     */
    @ConfigItem(defaultValue = "false")
    public boolean isLenient = false;

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     */
    @ConfigItem(defaultValue = "false")
    public boolean prettyPrint = false;

    /**
     * Specifies indent string to use with [prettyPrint] mode
     */
    @ConfigItem(defaultValue = "    ")
    public String prettyPrintIndent = "    ";

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     * <p>
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     */
    @ConfigItem(defaultValue = "true")
    public boolean useAlternativeNames = true;

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     */
    @ConfigItem(defaultValue = "false")
    public boolean useArrayPolymorphism = false;

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
    @ConfigItem(name = "naming-strategy")
    public Optional<String> namingStrategy;

    /**
     * Specifies if the enum values should be decoded case insensitively.
     */
    @ConfigItem(defaultValue = "false")
    public boolean decodeEnumsCaseInsensitive = false;

    /**
     * Specifies if trailing comma is allowed.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowTrailingComma = false;

    @Override
    public String toString() {
        return new StringJoiner(", ", JsonConfig.class.getSimpleName() + "[", "]")
                .add("encodeDefaults=" + encodeDefaults)
                .add("ignoreUnknownKeys=" + ignoreUnknownKeys)
                .add("isLenient=" + isLenient)
                .add("allowStructuredMapKeys=" + allowStructuredMapKeys)
                .add("prettyPrint=" + prettyPrint)
                .add("prettyPrintIndent='" + prettyPrintIndent + "'")
                .add("coerceInputValues=" + coerceInputValues)
                .add("useArrayPolymorphism=" + useArrayPolymorphism)
                .add("classDiscriminator='" + classDiscriminator + "'")
                .add("allowSpecialFloatingPointValues=" + allowSpecialFloatingPointValues)
                .add("useAlternativeNames=" + useAlternativeNames)
                .add("decodeEnumsCaseInsensitive=" + decodeEnumsCaseInsensitive)
                .add("allowTrailingComma=" + allowTrailingComma)
                .toString();
    }
}
