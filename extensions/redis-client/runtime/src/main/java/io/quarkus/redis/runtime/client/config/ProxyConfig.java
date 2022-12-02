package io.quarkus.redis.runtime.client.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.core.net.ProxyType;

@ConfigGroup
public class ProxyConfig {

    /**
     * Set proxy username.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Set proxy password.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Set proxy port. Defaults to 3128.
     */
    @ConfigItem(defaultValue = "3128")
    public int port;

    /**
     * Set proxy host.
     */
    @ConfigItem
    public String host;

    /**
     * Set proxy type.
     * Accepted values are: {@code HTTP} (default), {@code SOCKS4} and {@code SOCKS5}.
     */
    @ConfigItem(defaultValue = "http")
    public ProxyType type;

}
