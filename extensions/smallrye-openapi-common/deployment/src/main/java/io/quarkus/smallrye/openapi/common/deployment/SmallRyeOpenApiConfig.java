package io.quarkus.smallrye.openapi.common.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-openapi")
public final class SmallRyeOpenApiConfig {
    /**
     * The path at which to register the OpenAPI Servlet.
     */
    @ConfigItem(defaultValue = "/openapi")
    public String path;
}
