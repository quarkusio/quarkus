package io.quarkus.cxf.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
final class CxfConfig {

    /**
     * Set this to override the default path for CXF resources
     */
    @ConfigItem(defaultValue = "/")
    String path;

    /**
     * Choose the path of each web services.
     */
    @ConfigItem(name = "endpoint")
    Map<String, CxfEndpointConfig> endpoints;
}