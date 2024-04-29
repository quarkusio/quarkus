package io.quarkus.webdependency.locator.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for Web Dependency Locator.
 */
@ConfigRoot
public class WebDependencyLocatorConfig {

    /**
     * If the version reroute is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean versionReroute;

    /**
     * User defined import mappings
     */
    @ConfigItem
    @ConfigDocMapKey("module-specifier")
    public Map<String, String> importMappings;

    /**
     * The directory in the resources which serves as root for the web assets
     */
    @ConfigItem(defaultValue = "web")
    public String webRoot;

    /**
     * The directory in the resources which serves as root for the app assets
     */
    @ConfigItem(defaultValue = "app")
    public String appRoot;
}
