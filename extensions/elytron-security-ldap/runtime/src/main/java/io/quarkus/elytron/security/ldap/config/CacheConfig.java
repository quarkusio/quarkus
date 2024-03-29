package io.quarkus.elytron.security.ldap.config;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface CacheConfig {

    /**
     * If set to true, request to the LDAP server are cached
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The duration that an entry can stay in the cache
     */
    @WithDefault("60s")
    Duration maxAge();

    /**
     * The maximum number of entries to keep in the cache
     */
    @WithDefault("100")
    int size();

    String toString();
}
