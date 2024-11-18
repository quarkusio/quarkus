package io.quarkus.oidc.redis.token.state.manager.deployment;

import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * OIDC Redis Token State Manager build-time configuration.
 */
@ConfigRoot
@ConfigMapping(prefix = "quarkus.oidc.redis-token-state-manager")
public interface OidcRedisTokenStateManagerBuildConfig {

    /**
     * Enables this extension.
     * Set to 'false' if this extension should be disabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Selects Redis client used to store the OIDC token state.
     * The default Redis client is used if this property is not configured.
     * Used Redis datasource must only be accessible by trusted parties,
     * because Quarkus will not encrypt tokens before storing them.
     */
    @WithDefault(RedisConfig.DEFAULT_CLIENT_NAME)
    String redisClientName();

}
