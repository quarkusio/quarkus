package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration that allows for setting an HTTP header
 */
@ConfigGroup
public class HeaderConfig {

    /**
     * The path this header should be applied
     */
    @ConfigItem(defaultValue = "/*")
    public String path;

    /**
     * The value for this header configuration
     */
    @ConfigItem
    public String value;

    /**
     * The HTTP methods for this header configuration
     */
    @ConfigItem
    public Optional<List<String>> methods;
}
