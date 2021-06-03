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
     * Note that only Red Panda images are supported.
     * See https://vectorized.io/docs/quick-start-docker/ and https://hub.docker.com/r/vectorized/redpanda
     */
    @ConfigItem(defaultValue = "vectorized/redpanda:v21.5.5")
    public String imageName;

}
