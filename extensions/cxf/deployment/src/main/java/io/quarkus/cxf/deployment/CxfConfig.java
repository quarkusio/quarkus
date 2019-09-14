package io.quarkus.cxf.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
final class CxfConfig {

    /**
     * Set this to override the default path for JAX-RS resources if there are no
     * annotated application classes.
     */
    @ConfigItem(defaultValue = "/")
    String path;

    /**
     * Choose the path of each web services.
     */
    @ConfigItem(name = "webservice")
    Map<String, String> webServicesPaths;
}