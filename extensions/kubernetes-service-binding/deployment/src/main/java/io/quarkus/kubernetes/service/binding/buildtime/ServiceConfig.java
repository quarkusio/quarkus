package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ServiceConfig {

    /**
     * The kind of the service.
     */
    @ConfigItem
    public String kind;

    /**
     * The apiVersion of the service
     */
    @ConfigItem
    public String apiVersion;

    /**
     * The name of the service.
     */
    @ConfigItem
    public String name;

    /**
     * The namespace of the service.
     */
    @ConfigItem
    public Optional<String> namespace;

}
