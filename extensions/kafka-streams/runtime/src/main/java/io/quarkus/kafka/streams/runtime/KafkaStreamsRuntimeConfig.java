package io.quarkus.kafka.streams.runtime;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka-streams", phase = ConfigPhase.RUN_TIME)
public class KafkaStreamsRuntimeConfig {

    /**
     * Default Kafka bootstrap server.
     */
    public static final String DEFAULT_KAFKA_BROKER = "localhost:9092";

    /**
     * A unique identifier for this Kafka Streams application.
     * If not set, defaults to quarkus.application.name.
     */
    @ConfigItem(defaultValue = "${quarkus.application.name}")
    public String applicationId;

    /**
     * A comma-separated list of host:port pairs identifying the Kafka bootstrap server(s).
     * If not set, fallback to {@code kafka.bootstrap.servers}, and if not set either use {@code localhost:9092}.
     */
    @ConfigItem(defaultValue = DEFAULT_KAFKA_BROKER)
    public List<InetSocketAddress> bootstrapServers;

    /**
     * A unique identifier of this application instance, typically in the form host:port.
     */
    @ConfigItem
    public Optional<String> applicationServer;

    /**
     * A comma-separated list of topic names.
     * The pipeline will only be started once all these topics are present in the Kafka cluster
     * and {@code ignore.topics} is set to false.
     */
    @ConfigItem
    public Optional<List<String>> topics;

    /**
     * Timeout to wait for topic names to be returned from admin client.
     * If set to 0 (or negative), {@code topics} check is ignored.
     */
    @ConfigItem(defaultValue = "10S")
    public Duration topicsTimeout;

    /**
     * The schema registry key.
     *
     * Different schema registry libraries expect a registry URL
     * in different configuration properties.
     *
     * For Apicurio Registry, use {@code apicurio.registry.url}.
     * For Confluent schema registry, use {@code schema.registry.url}.
     */
    @ConfigItem(defaultValue = "schema.registry.url")
    public String schemaRegistryKey;

    /**
     * The schema registry URL.
     */
    @ConfigItem
    public Optional<String> schemaRegistryUrl;

    /**
     * The security protocol to use
     * See https://docs.confluent.io/current/streams/developer-guide/security.html#security-example
     */
    @ConfigItem(name = "security.protocol")
    public Optional<String> securityProtocol;

    /**
     * The SASL JAAS config.
     */
    public SaslConfig sasl;

    /**
     * Kafka SSL config
     */
    public SslConfig ssl;

    @Override
    public String toString() {
        return "KafkaStreamsRuntimeConfig{" +
                "applicationId='" + applicationId + '\'' +
                ", bootstrapServers=" + bootstrapServers +
                ", applicationServer=" + applicationServer +
                ", topics=" + topics +
                ", schemaRegistryKey='" + schemaRegistryKey + '\'' +
                ", schemaRegistryUrl=" + schemaRegistryUrl +
                ", sasl=" + sasl +
                ", ssl=" + ssl +
                '}';
    }

    public List<String> getTrimmedTopics() {
        return topics.orElseThrow(() -> new IllegalArgumentException("Missing list of topics"))
                .stream().map(String::trim).collect(Collectors.toList());
    }
}
