package io.quarkus.cxf.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CxfEndpointConfig {

    /**
     * The server class implementor
     */
    @ConfigItem
    public Optional<String> implementor;

    /**
     * The wsdl path
     */
    @ConfigItem
    public Optional<String> wsdlPath;

    /**
     * The client endpoint url
     */
    @ConfigItem
    public Optional<String> clientEndpointURL;

    /**
     * The client interface
     */
    @ConfigItem
    public Optional<String> serviceInterface;
}
