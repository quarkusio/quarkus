package io.quarkus.apicurio.registry.devservice;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "apicurio-registry", phase = ConfigPhase.BUILD_TIME)
public class ApicurioRegistryBuildTimeConfig {

    /**
     * Dev Services
     * <p>
     * Dev Services allows Quarkus to automatically start Apicurio Registry in dev and test mode.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public ApicurioRegistryDevServicesBuildTimeConfig devservices;

    @ConfigGroup
    public static class ApicurioRegistryDevServicesBuildTimeConfig {

        /**
         * If Dev Services for Apicurio Registry has been explicitly enabled or disabled. Dev Services are generally enabled
         * by default, unless there is an existing configuration present. For Apicurio Registry, Dev Services starts a registry
         * unless {@code mp.messaging.connector.smallrye-kafka.apicurio.registry.url} or
         * {@code mp.messaging.connector.smallrye-kafka.schema.registry.url} is set.
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
         * Specifically, the image repository must end with {@code apicurio/apicurio-registry-mem}.
         */
        @ConfigItem(defaultValue = "quay.io/apicurio/apicurio-registry-mem:2.4.2.Final")
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
         * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
         * starts a new container with the {@code quarkus-dev-service-apicurio-registry} label set to the specified value.
         * <p>
         * This property is used when you need multiple shared Apicurio Registry instances.
         */
        @ConfigItem(defaultValue = "apicurio-registry")
        public String serviceName;

        /**
         * Environment variables that are passed to the container.
         */
        @ConfigItem
        @ConfigDocMapKey("environment-variable-name")
        public Map<String, String> containerEnv;
    }
}
