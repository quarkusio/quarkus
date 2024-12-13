package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * Configuration that allows for setting an HTTP header
 */
public interface HeaderConfig {
    /**
     * The path this header should be applied
     */
    @WithDefault("/*")
    String path();

    /**
     * The value for this header configuration
     */
    String value();

    /**
     * The HTTP methods for this header configuration
     */
    Optional<List<String>> methods();
}
