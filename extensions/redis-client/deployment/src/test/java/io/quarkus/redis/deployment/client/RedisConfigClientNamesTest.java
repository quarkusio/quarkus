package io.quarkus.redis.deployment.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.deployment.client.RedisBuildTimeConfig.DevServiceConfiguration;
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
                createDevServiceConfiguration(false),
                Map.of());

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).isEmpty();
    }

    @Test
    void testWithDefaultConfig() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                createDevServiceConfiguration(true),
                Map.of());

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithAdditionalDevServices() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                createDevServiceConfiguration(true),
                Map.of("additional", createDevServiceConfiguration(true)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>", "additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceConfig() {
        createConfig(Map.of());

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                createDevServiceConfiguration(false),
                Map.of("additional", createDevServiceConfiguration(true)));

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithHostsConfig() {
        createConfig(Map.of("quarkus.redis.hosts", "redis://localhost:1234"));

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                createDevServiceConfiguration(false),
                Map.of());

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithAdditionalHostsConfig() {
        createConfig(Map.of("quarkus.redis.my-redis.hosts", "redis://localhost:5678",
                "quarkus.redis.my-redis-2.hosts-provider-name", "my-redis-2-provider"));

        RedisBuildTimeConfig redisBuildTimeConfig = createRedisBuildTimeConfig(
                createDevServiceConfiguration(false),
                Map.of());

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("my-redis", "my-redis-2");
    }

    private static RedisBuildTimeConfig createRedisBuildTimeConfig(DevServiceConfiguration defaultDevService,
            Map<String, DevServiceConfiguration> additionalDevServices) {
        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig() {

            @Override
            public RedisClientBuildTimeConfig defaultRedisClient() {
                return null;
            }

            @Override
            public Map<String, RedisClientBuildTimeConfig> namedRedisClients() {
                return Map.of();
            }

            @Override
            public boolean healthEnabled() {
                return false;
            }

            @Override
            public DevServiceConfiguration defaultDevService() {
                return defaultDevService;
            }

            @Override
            public Map<String, DevServiceConfiguration> additionalDevServices() {
                return additionalDevServices;
            }
        };

        return redisBuildTimeConfig;
    }

    private static DevServiceConfiguration createDevServiceConfiguration(boolean enabled) {
        DevServicesConfig devServicesConfig = new DevServicesConfig() {

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public Optional<String> imageName() {
                return Optional.empty();
            }

            @Override
            public OptionalInt port() {
                return OptionalInt.empty();
            }

            @Override
            public boolean shared() {
                return false;
            }

            @Override
            public String serviceName() {
                return null;
            }

            @Override
            public Map<String, String> containerEnv() {
                return Map.of();
            }
        };

        DevServiceConfiguration devServiceConfiguration = new DevServiceConfiguration() {

            @Override
            public DevServicesConfig devservices() {
                return devServicesConfig;
            }
        };

        return devServiceConfiguration;
    }
}
