package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-openapi")
public final class SmallRyeOpenApiConfig {
    /**
     * The path at which to register the OpenAPI Servlet.
     */
    @ConfigItem(defaultValue = "openapi")
    public String path;

    /**
     * If set, the generated OpenAPI schema documents will be stored here on build.
     * Both openapi.json and openapi.yaml will be stored here if this is set.
     */
    @ConfigItem
    public Optional<Path> storeSchemaDirectory;
}
