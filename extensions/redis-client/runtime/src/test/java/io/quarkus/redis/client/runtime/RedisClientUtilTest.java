package io.quarkus.redis.client.runtime;

import static io.quarkus.redis.client.runtime.RedisClientUtil.DEFAULT_CLIENT;
import static io.quarkus.redis.client.runtime.RedisClientUtil.getConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;

class RedisClientUtilTest {

    @Test
    void testGetConfiguration() {
        RedisConfig config = new RedisConfig();
        config.defaultClient = new RedisConfiguration();
        config.additionalRedisClients = new HashMap<>();
        RedisConfiguration namedConfiguration = new RedisConfiguration();
        config.additionalRedisClients.put("some-configuration", namedConfiguration);

        // get default client configuration
        RedisConfiguration configuration = getConfiguration(config, DEFAULT_CLIENT);
        assertThat(configuration).isEqualTo(config.defaultClient);

        // get named client configuration
        configuration = getConfiguration(config, "some-configuration");
        assertThat(configuration).isEqualTo(namedConfiguration);

        // throw error for non-existing named client
        assertThatThrownBy(() -> {
            getConfiguration(config, "non-existing");
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
