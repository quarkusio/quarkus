package io.quarkus.kafka.client.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class KafkaDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Kafka has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For Kafka, Dev Services starts a broker unless
     * {@code kafka.bootstrap.servers} is set or if all the Reactive Messaging Kafka channel are configured with a
     * {@code bootstrap.servers}.
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
     * The Kafka image to use.
     * Note that only Redpanda images are supported.
     * See https://vectorized.io/docs/quick-start-docker/ and https://hub.docker.com/r/vectorized/redpanda
     */
    @ConfigItem(defaultValue = "vectorized/redpanda:v21.5.5")
    public String imageName;

    /**
     * Indicates if the Kafka broker managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Kafka starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-kafka} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-kafka} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Kafka looks for a container with the
     * {@code quarkus-dev-service-kafka} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-kafka} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Kafka brokers.
     */
    @ConfigItem(defaultValue = "kafka")
    public String serviceName;

}
