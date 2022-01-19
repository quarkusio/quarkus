package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ServiceConfig {

    /**
     * The name of the service binding.
     * If no value is specified the id of the service will be used instead.
     */
    @ConfigItem
    public Optional<String> binding;

    /**
     * The kind of the service.
     */
    @ConfigItem
    public Optional<String> kind;

    /**
     * The apiVersion of the service
     */
    @ConfigItem
    public Optional<String> apiVersion;

    /**
     * The name of the service.
     * When this is empty the key of the service is meant to be used as name.
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * The namespace of the service.
     */
    @ConfigItem
    public Optional<String> namespace;

    protected static ServiceConfig createNew() {
        ServiceConfig config = new ServiceConfig();
        config.binding = Optional.empty();
        config.apiVersion = Optional.empty();
        config.kind = Optional.empty();
        config.name = Optional.empty();
        config.namespace = Optional.empty();
        return config;
    }

    protected ServiceConfig withBinding(String binding) {
        this.binding = Optional.of(binding);
        return this;
    }

    protected ServiceConfig withApiVersion(String apiVersion) {
        this.apiVersion = Optional.of(apiVersion);
        return this;
    }

    protected ServiceConfig withKind(String kind) {
        this.kind = Optional.of(kind);
        return this;
    }

    protected ServiceConfig withName(String name) {
        this.name = Optional.of(name);
        return this;
    }

}
