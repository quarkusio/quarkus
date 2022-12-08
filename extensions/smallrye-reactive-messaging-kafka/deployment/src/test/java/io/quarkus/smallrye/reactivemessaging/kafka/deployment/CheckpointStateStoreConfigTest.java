package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import static io.quarkus.smallrye.reactivemessaging.kafka.HibernateOrmStateStore.HIBERNATE_ORM_STATE_STORE;
import static io.quarkus.smallrye.reactivemessaging.kafka.HibernateReactiveStateStore.HIBERNATE_REACTIVE_STATE_STORE;
import static io.quarkus.smallrye.reactivemessaging.kafka.RedisStateStore.REDIS_STATE_STORE;
import static io.quarkus.smallrye.reactivemessaging.kafka.deployment.SmallRyeReactiveMessagingKafkaProcessor.hasStateStoreConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class CheckpointStateStoreConfigTest {

    SmallRyeConfig config;

    @AfterEach
    void tearDown() {
        if (config != null) {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    private void createConfig(Map<String, String> configMap) {
        config = new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", configMap) {
                })
                .build();
    }

    @Test
    void testHasStateStoreConfigWithConnectorConfig() {
        createConfig(Map.of("mp.messaging.connector.smallrye-kafka.checkpoint.state-store", HIBERNATE_ORM_STATE_STORE));
        assertTrue(hasStateStoreConfig(HIBERNATE_ORM_STATE_STORE, config));
    }

    @Test
    void testHasStateStoreConfigWithChannelConfig() {
        createConfig(Map.of("mp.messaging.incoming.my-channel.checkpoint.state-store", HIBERNATE_REACTIVE_STATE_STORE));
        assertTrue(hasStateStoreConfig(HIBERNATE_REACTIVE_STATE_STORE, config));
    }

    @Test
    void testHasStateStoreConfigWithInvalidChannelConfig() {
        createConfig(Map.of(
                "mp.messaging.outgoing.my-channel.checkpoint.state-store", HIBERNATE_REACTIVE_STATE_STORE,
                "mp.messaging.incoming.my-channel.state-store", HIBERNATE_ORM_STATE_STORE));
        assertFalse(hasStateStoreConfig(HIBERNATE_REACTIVE_STATE_STORE, config));
        assertFalse(hasStateStoreConfig(HIBERNATE_ORM_STATE_STORE, config));
    }

    @Test
    void testHasStateStoreConfigEmptyConfig() {
        createConfig(Map.of());
        assertFalse(hasStateStoreConfig(REDIS_STATE_STORE, config));
    }
}
