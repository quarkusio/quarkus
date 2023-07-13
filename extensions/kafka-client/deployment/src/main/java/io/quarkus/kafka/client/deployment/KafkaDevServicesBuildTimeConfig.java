package io.quarkus.kafka.client.deployment;

import java.time.Duration;
import java.util.Map;
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
     * Kafka dev service container type.
     * <p>
     * Redpanda, Strimzi and kafka-native container providers are supported. Default is redpanda.
     * <p>
     * For Redpanda:
     * See https://vectorized.io/docs/quick-start-docker/ and https://hub.docker.com/r/vectorized/redpanda
     * <p>
     * For Strimzi:
     * See https://github.com/strimzi/test-container and https://quay.io/repository/strimzi-test-container/test-container
     * <p>
     * For Kafka Native:
     * See https://github.com/ozangunalp/kafka-native and https://quay.io/repository/ogunalp/kafka-native
     * <p>
     * Note that Strimzi and Kafka Native images are launched in Kraft mode.
     */
    @ConfigItem(defaultValue = "redpanda")
    public Provider provider = Provider.REDPANDA;

    public enum Provider {
        REDPANDA("docker.io/vectorized/redpanda:v22.3.4"),
        STRIMZI("quay.io/strimzi-test-container/test-container:latest-kafka-3.2.1"),
        KAFKA_NATIVE("quay.io/ogunalp/kafka-native:latest");

        private final String defaultImageName;

        Provider(String imageName) {
            this.defaultImageName = imageName;
        }

        public String getDefaultImageName() {
            return defaultImageName;
        }
    }

    /**
     * The Kafka container image to use.
     * <p>
     * Dependent on the provider.
     */
    @ConfigItem
    public Optional<String> imageName;

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
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-kafka} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Kafka brokers.
     */
    @ConfigItem(defaultValue = "kafka")
    public String serviceName;

    /**
     * The topic-partition pairs to create in the Dev Services Kafka broker.
     * After the broker is started, given topics with partitions are created, skipping already existing topics.
     * For example, <code>quarkus.kafka.devservices.topic-partitions.test=2</code> will create a topic named
     * {@code test} with 2 partitions.
     * <p>
     * The topic creation will not try to re-partition existing topics with different number of partitions.
     */
    @ConfigItem
    public Map<String, Integer> topicPartitions;

    /**
     * Timeout for admin client calls used in topic creation.
     * <p>
     * Defaults to 2 seconds.
     */
    @ConfigItem(defaultValue = "2S")
    public Duration topicPartitionsTimeout;

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigItem
    public Map<String, String> containerEnv;

    /**
     * Allows configuring the Red Panda broker.
     */
    @ConfigItem
    public RedPandaBuildTimeConfig redpanda;

}
