package io.quarkus.cxf.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CxfEndpointConfig {

    /**
     * The class implementor
     */
    @ConfigItem
    public String implementor;
}
