package io.quarkus.proxy;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface ProxyConfiguration {
    /**
     * Proxy host
     */
    String host();

    /**
     * Proxy port
     */
    int port();

    /**
     * Proxy username
     */
    Optional<String> username();

    /**
     * Proxy password
     */
    Optional<String> password();

    /**
     * Hostnames or IP addresses to exclude from proxying.
     */
    Optional<List<String>> nonProxyHosts();

    /**
     * Proxy connection timeout.
     */
    Optional<Duration> proxyConnectTimeout();

    /**
     * Proxy type.
     */
    ProxyType type();

    /**
     * @return this {@link ProxyConfiguration} if {@link #type()} returns {@link ProxyType#HTTP};
     *         otherwise throws {@link IllegalStateException}
     * @throws IllegalStateException if {@link #type()} does not return {@link ProxyType#HTTP}
     */
    default ProxyConfiguration assertHttpType() {
        if (type() != ProxyType.HTTP) {
            throw new IllegalStateException("Proxy type HTTP is required");
        }
        return this;
    }
}
