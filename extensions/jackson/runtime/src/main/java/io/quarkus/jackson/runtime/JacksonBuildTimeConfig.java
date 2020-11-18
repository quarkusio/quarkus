package io.quarkus.jackson.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class JacksonBuildTimeConfig {

    /**
     * If enabled, Jackson will fail when encountering unknown properties.
     * <p>
     * You can still override it locally with {@code @JsonIgnoreProperties(ignoreUnknown = false)}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean failOnUnknownProperties;
}
