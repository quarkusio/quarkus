package io.quarkus.smallrye.openapi.common.deployment;

import io.smallrye.config.WithDefault;

public interface SmallRyeOpenApiJsClientConfig {

    /**
     * Generate a JavaScript client proxy for all REST operations discovered by OpenAPI.
     * When enabled, a static client library and a typed proxy module are generated
     * as web resources, along with an import map for bare module imports.
     */
    @WithDefault("false")
    boolean enabled();
}
