package io.quarkus.narayana.lra.deployment.devservice;

import java.util.Map;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface LRACoordinatorDevServicesBuildTimeConfig {

    /**
     * If Dev Services for the LRA coordinator has been explicitly enabled or disabled. For the LRA coordinator,
     * the Dev Services is disabled if this property is false or if the {@code quarkus.lra.coordinator-url}
     * configuration property is defined.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Optional fixed port the Dev Services will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt port();

    /**
     * Optional override of the LRA coordinator container image to use.
     */
    @WithDefault("quay.io/jbosstm/lra-coordinator:7.2.2.Final-3.25.0")
    String imageName();

    /**
     * Indicates if the LRA coordinator managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for LRA coordinator starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-lra-coordinator} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-lra-coordinator} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for LRA coordinator looks for a container with the
     * {@code quarkus-dev-service-lra-coordinator} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-lra-coordinator} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared LRA coordinators.
     */
    @WithDefault("lra-coordinator")
    String serviceName();

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();

    /**
     * Whether to log the warning messages about the LRA Dev Services.
     * <p>
     * Defaults to {@code true}.
     */
    @WithDefault("true")
    boolean logWarning();
}
