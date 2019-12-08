package io.quarkus.kafka.client.runtime;

import java.net.InetSocketAddress;
import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * For now, this is not used except to avoid a warning for the health check.
 */
@ConfigRoot(name = "kafka", phase = ConfigPhase.RUN_TIME)
public class KafkaRuntimeConfig {

    /**
     * A comma-separated list of host:port pairs identifying the Kafka bootstrap server(s)
     */
    @ConfigItem(defaultValue = "localhost:9012")
    public List<InetSocketAddress> bootstrapServers;
}
