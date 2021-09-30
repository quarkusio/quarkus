package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JacksonBuildTimeConfig {

    /**
     * If enabled, Jackson will fail when encountering unknown properties.
     * <p>
     * You can still override it locally with {@code @JsonIgnoreProperties(ignoreUnknown = false)}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean failOnUnknownProperties;

    /**
     * If enabled, Jackson will fail when no accessors are found for a type.
     * This is enabled by default to match the default Jackson behavior.
     */
    @ConfigItem(defaultValue = "true")
    public boolean failOnEmptyBeans;

    /**
     * If enabled, Jackson will serialize dates as numeric value(s).
     */
    @ConfigItem(defaultValue = "false")
    public boolean writeDatesAsTimestamps;

    /**
     * If enabled, Jackson will ignore case during Enum deserialization.
     */
    @ConfigItem(defaultValue = "false")
    public boolean acceptCaseInsensitiveEnums;

    /**
     * If set, Jackson will default to using the specified timezone when formatting dates.
     * Some examples values are "Asia/Jakarta" and "GMT+3".
     * If not set, Jackson will use its own default.
     */
    @ConfigItem(defaultValue = "UTC")
    public Optional<ZoneId> timezone;

    /**
     * Define which properties of Java Beans are to be included in serialization.
     */
    @ConfigItem
    public Optional<JsonInclude.Include> serializationInclusion;
}
