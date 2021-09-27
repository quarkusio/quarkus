package io.quarkus.apicurio.registry.avro;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "apicurio-registry.devservices", phase = ConfigPhase.BUILD_TIME)
public class ApicurioRegistryDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Apicurio Registry has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Apicurio Registry, Dev Services starts a registry
     * unless {@code mp.messaging.connector.smallrye-kafka.apicurio.registry.url} is set.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The Apicurio Registry image to use.
     * Note that only Apicurio Registry 2.x images are supported.
     */
    @ConfigItem(defaultValue = "apicurio/apicurio-registry-mem:2.0.1.Final")
    public String imageName;

    /**
     * Indicates if the Apicurio Registry instance managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Apicurio Registry starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-apicurio-registry} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-apicurio-registry} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Apicurio Registry looks for a container with the
     * {@code quarkus-dev-service-apicurio-registry} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-apicurio-registry} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Apicurio Registry instances.
     */
    @ConfigItem(defaultValue = "apicurio-registry")
    public String serviceName;

}
