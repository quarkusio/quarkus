package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface ContainerConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a containers when running in Dev or Test mode and when Docker is running.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The container image name to use, for container based DevServices providers.
     * <p>
     * Ignored for the config doc here as a more precise value will be defined in subinterfaces.
     */
    @ConfigDocIgnore
    String imageName();

    /**
     * Indicates if the container managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-label} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * Network aliases.
     * <p>
     * Ignored for the config doc here as a more precise value will be defined in subinterfaces.
     */
    @ConfigDocIgnore
    Optional<Set<String>> networkAliases();

    /**
     * The full name of the label attached to the started container.
     * This label is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for looks for a container with th label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with this label set to the specified value.
     * <p>
     * This property is used when you need multiple shared containers.
     * <p>
     * Ignored for the config doc here as a more precise value will be defined in subinterfaces.
     */
    @ConfigDocIgnore
    String label();

    /**
     * The value of the {@code quarkus-dev-service} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for looks for a container with the
     * {@code quarkus-dev-service} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared containers.
     */
    @WithDefault("quarkus")
    String serviceName();
}
