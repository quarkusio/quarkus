package io.quarkus.kafka.streams.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.kafka-streams")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KafkaStreamsBuildTimeConfig {

    /**
     * Whether a health check is published in case the smallrye-health extension is present (defaults to true).
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    /**
     * Whether RocksDB state store support is enabled.
     * Set to {@code false} to use in-memory state stores only, which removes the RocksDB native
     * library dependency. This is useful for ARM native builds where RocksDB native libraries
     * may not be available.
     * When disabled, you must configure Kafka Streams to use in-memory stores, e.g. by setting
     * {@code quarkus.kafka-streams.dsl.store.suppliers.class} to
     * {@code org.apache.kafka.streams.state.BuiltInDslStoreSuppliers$InMemoryDslStoreSuppliers}.
     */
    @WithName("rocksdb.enabled")
    @WithDefault("true")
    boolean rocksDbEnabled();
}
