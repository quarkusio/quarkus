package io.quarkus.redis.runtime.client.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.vertx.core.net.ProxyType;

@ConfigGroup
public interface ProxyConfig {

    /**
     * Set proxy username.
     * Honored only when {@code quarkus.redis.tcp.proxy-options.host} is set.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @Deprecated
    Optional<String> username();

    /**
     * Set proxy password.
     * Honored only when {@code quarkus.redis.tcp.proxy-options.host} is set.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @Deprecated
    Optional<String> password();

    /**
     * Set proxy port. Defaults to 3128.
     * Honored only when {@code quarkus.redis.tcp.proxy-options.host} is set.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @WithDefault("3128")
    @Deprecated
    int port();

    /**
     * Set proxy host.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @Deprecated
    Optional<String> host();

    /**
     * Set proxy type.
     * Accepted values are: {@code HTTP} (default), {@code SOCKS4} and {@code SOCKS5}.
     * Honored only when {@code quarkus.redis.tcp.proxy-options.host} is set.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @WithDefault("http")
    @Deprecated
    ProxyType type();

}
