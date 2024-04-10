package io.quarkus.webjar.locator.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build time configuration for WebJar Locator.
 */
@ConfigRoot
public class WebJarLocatorConfig {

    /**
     * If the version reroute is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean versionReroute;

    /**
     * User defined import mappings
     */
    @ConfigItem
    public Map<String, String> importMappings;

}
