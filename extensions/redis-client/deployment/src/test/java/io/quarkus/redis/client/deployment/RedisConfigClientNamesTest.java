package io.quarkus.redis.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
        DevServicesConfig devservices = new DevServicesConfig();

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservices;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;
        redisBuildTimeConfig.additionalDevServices = Map.of();

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).isEmpty();
    }

    @Test
    void testWithDefaultConfig() {
        createConfig(Map.of());
        DevServicesConfig devservices = new DevServicesConfig();
        devservices.enabled = true;

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservices;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;
        redisBuildTimeConfig.additionalDevServices = Map.of();

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithAdditionalDevServices() {
        createConfig(Map.of());
        DevServicesConfig devservicesCfg = new DevServicesConfig();
        devservicesCfg.enabled = true;

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservicesCfg;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;

        DevServicesConfig additionalDevService = new DevServicesConfig();
        additionalDevService.enabled = true;

        RedisBuildTimeConfig.DevServiceConfiguration additional = new RedisBuildTimeConfig.DevServiceConfiguration();
        additional.devservices = additionalDevService;
        redisBuildTimeConfig.additionalDevServices = Map.of("additional", additional);

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>", "additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceConfig() {
        createConfig(Map.of());
        DevServicesConfig devservicesCfg = new DevServicesConfig();
        devservicesCfg.enabled = false;

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservicesCfg;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;

        DevServicesConfig additionalDevService = new DevServicesConfig();
        additionalDevService.enabled = true;

        RedisBuildTimeConfig.DevServiceConfiguration additional = new RedisBuildTimeConfig.DevServiceConfiguration();
        additional.devservices = additionalDevService;
        redisBuildTimeConfig.additionalDevServices = Map.of("additional", additional);

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("additional");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithHostsConfig() {
        createConfig(Map.of("quarkus.redis.hosts", "redis://localhost:1234"));
        DevServicesConfig devservicesCfg = new DevServicesConfig();
        devservicesCfg.enabled = false;

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservicesCfg;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;

        redisBuildTimeConfig.additionalDevServices = Map.of();

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("<default>");
    }

    @Test
    void testWithDisabledDefaultDevServiceWithAdditionalHostsConfig() {
        createConfig(Map.of("quarkus.redis.my-redis.hosts", "redis://localhost:5678",
                "quarkus.redis.my-redis-2.hosts-provider-name", "my-redis-2-provider"));
        DevServicesConfig devservicesCfg = new DevServicesConfig();
        devservicesCfg.enabled = false;

        RedisBuildTimeConfig.DevServiceConfiguration devService = new RedisBuildTimeConfig.DevServiceConfiguration();
        devService.devservices = devservicesCfg;

        RedisBuildTimeConfig redisBuildTimeConfig = new RedisBuildTimeConfig();
        redisBuildTimeConfig.defaultDevService = devService;

        redisBuildTimeConfig.additionalDevServices = Map.of();

        Set<String> names = RedisClientProcessor.configuredClientNames(redisBuildTimeConfig, config);
        assertThat(names).containsOnly("my-redis", "my-redis-2");
    }
}
