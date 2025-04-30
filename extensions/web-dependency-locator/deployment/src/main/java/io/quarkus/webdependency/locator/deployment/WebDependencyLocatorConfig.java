package io.quarkus.webdependency.locator.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for Web Dependency Locator.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.web-dependency-locator")
public interface WebDependencyLocatorConfig {

    /**
     * If the version reroute is enabled.
     */
    @WithDefault("true")
    boolean versionReroute();

    /**
     * User defined import mappings
     */
    @ConfigDocMapKey("module-specifier")
    Map<String, String> importMappings();

    /**
     * The directory in the resources which serves as root for the web assets
     */
    @WithDefault("web")
    String webRoot();

    /**
     * The directory in the resources which serves as root for the app assets
     */
    @WithDefault("app")
    String appRoot();
}
