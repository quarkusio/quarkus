package io.quarkus.kafka.streams.runtime;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.kafka-streams")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface KafkaStreamsRuntimeConfig {

    /**
     * Default Kafka bootstrap server.
     */
    String DEFAULT_KAFKA_BROKER = "localhost:9092";

    /**
     * A unique identifier for this Kafka Streams application.
     * If not set, defaults to quarkus.application.name.
     */
    @WithDefault("${quarkus.application.name}")
    String applicationId();

    /**
     * A comma-separated list of host:port pairs identifying the Kafka bootstrap server(s).
     * If not set, fallback to {@code kafka.bootstrap.servers}, and if not set either use {@code localhost:9092}.
     */
    @WithDefault(DEFAULT_KAFKA_BROKER)
    List<InetSocketAddress> bootstrapServers();

    /**
     * A unique identifier of this application instance, typically in the form host:port.
     */
    Optional<String> applicationServer();

    /**
     * A comma-separated list of topic names.
     * The pipeline will only be started once all these topics are present in the Kafka cluster
     * and {@code ignore.topics} is set to false.
     */
    Optional<List<String>> topics();

    /**
     * A comma-separated list of topic name patterns.
     * The pipeline will only be started once all these topics are present in the Kafka cluster
     * and {@code ignore.topics} is set to false.
     */
    Optional<List<String>> topicPatterns();

    /**
     * Timeout to wait for topic names to be returned from admin client.
     * If set to 0 (or negative), {@code topics} check is ignored.
     */
    @WithDefault("10S")
    Duration topicsTimeout();

    /**
     * The schema registry key.
     *
     * Different schema registry libraries expect a registry URL
     * in different configuration properties.
     *
     * For Apicurio Registry, use {@code apicurio.registry.url}.
     * For Confluent schema registry, use {@code schema.registry.url}.
     */
    @WithDefault("schema.registry.url")
    String schemaRegistryKey();

    /**
     * The schema registry URL.
     */
    Optional<String> schemaRegistryUrl();

    /**
     * The security protocol to use
     * See https://docs.confluent.io/current/streams/developer-guide/security.html#security-example
     */
    @WithName("security.protocol")
    Optional<String> securityProtocol();

    /**
     * The SASL JAAS config.
     */
    SaslConfig sasl();

    /**
     * Kafka SSL config
     */
    SslConfig ssl();

}
