package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.jackson")
public interface JacksonBuildTimeConfig {

    /**
     * If enabled, Jackson will fail when encountering unknown properties.
     * <p>
     * You can still override it locally with {@code @JsonIgnoreProperties(ignoreUnknown = false)}.
     */
    @WithDefault("false")
    boolean failOnUnknownProperties();

    /**
     * If enabled, Jackson will fail when no accessors are found for a type.
     * This is enabled by default to match the default Jackson behavior.
     */
    @WithDefault("true")
    boolean failOnEmptyBeans();

    /**
     * If enabled, Jackson will serialize dates as numeric value(s).
     * When disabled, they are serialized in ISO 8601 format.
     */
    @WithDefault("false")
    boolean writeDatesAsTimestamps();

    /**
     * If enabled, Jackson will serialize durations as numeric value(s).
     * When disabled, they are serialized in ISO 8601 format.
     * This is enabled by default to match the default Jackson behavior.
     */
    @WithDefault("true")
    boolean writeDurationsAsTimestamps();

    /**
     * If enabled, Jackson will ignore case during Enum deserialization.
     */
    @WithDefault("false")
    boolean acceptCaseInsensitiveEnums();

    /**
     * If set, Jackson will default to using the specified timezone when formatting dates.
     * Some examples values are "Asia/Jakarta" and "GMT+3".
     */
    @WithDefault("UTC")
    ZoneId timezone();

    /**
     * Define which properties of Java Beans are to be included in serialization.
     */
    Optional<JsonInclude.Include> serializationInclusion();

    /**
     * Defines how names of JSON properties ("external names") are derived
     * from names of POJO methods and fields ("internal names").
     * The value can be one of the one of the constants in {@link com.fasterxml.jackson.databind.PropertyNamingStrategies},
     * so for example, {@code LOWER_CAMEL_CASE} or {@code UPPER_CAMEL_CASE}.
     * <p>
     * The value can also be a fully qualified class name of a {@link com.fasterxml.jackson.databind.PropertyNamingStrategy}
     * subclass.
     */
    Optional<String> propertyNamingStrategy();
}
