package io.quarkus.cxf.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CxfEndpointConfig {

    /**
     * The class implementor
     */
    @ConfigItem
    public String implementor;

    /**
     * The wsdl path
     */
    @ConfigItem
    public Optional<String> wsdlPath;
}
