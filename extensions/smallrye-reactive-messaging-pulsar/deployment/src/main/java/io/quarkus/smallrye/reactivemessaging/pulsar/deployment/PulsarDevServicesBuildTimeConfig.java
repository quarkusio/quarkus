package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface PulsarDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Pulsar has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Pulsar, Dev Services starts a broker unless
     * {@code pulsar.client.serviceUrl} is set or if all the Reactive Messaging Pulsar channel are configured with
     * {@code serviceUrl}.
     */
    Optional<Boolean> enabled();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    Optional<Integer> port();

    /**
     * The image to use.
     * Note that only Apache Pulsar images are supported.
     * Specifically, the image repository must end with {@code apachepulsar/pulsar}.
     *
     * Check https://hub.docker.com/r/apachepulsar/pulsar to find the available versions.
     */
    // Alpine-based images starting from 3.3.0 fail to start on aarch64: https://github.com/apache/pulsar/issues/23306
    @WithDefault("apachepulsar/pulsar:3.2.4")
    String imageName();

    /**
     * Indicates if the Pulsar broker managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Pulsar starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-pulsar} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-pulsar} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Pulsar looks for a container with the
     * {@code quarkus-dev-service-pulsar} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-pulsar} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Pulsar brokers.
     */
    @WithDefault("pulsar")
    String serviceName();

    /**
     * Broker config to set on the Pulsar instance
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> brokerConfig();

}
