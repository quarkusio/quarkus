package io.quarkus.redis.deployment.client;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigInstanceBuilder;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class RedisConfigClientNamesTest {

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
    void testWithEmptyConfig() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(false)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).isEmpty();
    }

    @Test
    void testWithDefaultConfig() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(true)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithAdditionalDevServices() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(true), "additional",
                        createDevServiceConfiguration(true)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>", "additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceConfig() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(false), "additional",
                        createDevServiceConfiguration(true)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithHostsConfig() {
        createConfig(Map.of("quarkus.redis.hosts", "redis://localhost:1234"));

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(false)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithAdditionalHostsConfig() {
        createConfig(Map.of("quarkus.redis.my-redis.hosts", "redis://localhost:5678",
                "quarkus.redis.my-redis-2.hosts-provider-name", "my-redis-2-provider"));

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(false)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("my-redis", "my-redis-2");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithValkeyHostsConfig() {
        createConfig(Map.of("quarkus.redis.hosts", "valkey://localhost:1234",
                "quarkus.redis.valkey-client.hosts", "valkeys://localhost:5678"));

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                Map.of(DEFAULT_CLIENT_NAME, createDevServiceConfiguration(false)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>", "valkey-client");
    }

    private static RedisBuildTimeConfig createRedisBuildTimeConfig(Map<String, DevServicesConfig> devServices) {
        Map<String, RedisClientBuildTimeConfig> clients = new HashMap<>();
        for (String name : devServices.keySet()) {
            clients.put(name, ConfigInstanceBuilder.forInterface(RedisClientBuildTimeConfig.class)
                    .with(RedisClientBuildTimeConfig::flushBeforeLoad, false)
                    .with(RedisClientBuildTimeConfig::loadOnlyIfEmpty, false)
                    .with(RedisClientBuildTimeConfig::devservices, devServices.get(name))
                    .build());
        }
        return ConfigInstanceBuilder.forInterface(RedisBuildTimeConfig.class)
                .with(RedisBuildTimeConfig::clients, clients)
                .with(RedisBuildTimeConfig::healthEnabled, false)
                .build();
    }

    private static DevServicesConfig createDevServiceConfiguration(boolean enabled) {
        return ConfigInstanceBuilder.forInterface(DevServicesConfig.class)
                .with(DevServicesConfig::enabled, enabled)
                .with(DevServicesConfig::shared, false)
                .build();
    }
}
