package io.quarkus.kafka.client.deployment;

import java.util.Map;

/**
 * Allows configuring the Strimzi dev services broker.
 */
public interface StrimziBuildTimeConfig {

    /**
     * Configuration properties that will be added to the server properties
     * file provided to the Kafka broker by the Strimzi provider.
     */
    Map<String, String> serverConfigs();
}
