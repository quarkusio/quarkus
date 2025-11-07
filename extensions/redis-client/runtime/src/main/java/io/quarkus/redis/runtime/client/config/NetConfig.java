package io.quarkus.redis.runtime.client.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface NetConfig {

    /**
     * Set the ALPN usage.
     */
    Optional<Boolean> alpn();

    /**
     * Sets the list of application-layer protocols to provide to the server during the
     * {@code Application-Layer Protocol Negotiation}.
     */
    Optional<List<String>> applicationLayerProtocols();

    /**
     * Sets the list of enabled SSL/TLS protocols.
     */
    Optional<Set<String>> secureTransportProtocols();

    /**
     * Set the idle timeout.
     */
    Optional<Duration> idleTimeout();

    /**
     * Set the connect timeout.
     */
    Optional<Duration> connectionTimeout();

    /**
     * The name of the proxy configuration to use.
     * <p>
     * If {@code quarkus.redis.tcp.proxy-options.host} is set, the {@code quarkus.redis.tcp.proxy-options.*}
     * (and {@code quarkus.redis.tcp.non-proxy-hosts}) configuration is used and the {@code quarkus.proxy.*}
     * configuration is ignored.
     * <p>
     * Otherwise, if not set and the default proxy configuration is configured ({@code quarkus.proxy.*}), it is used.
     * If set to {@code none}, no proxy configuration is used.
     * If set to some other value, the configuration from {@code quarkus.proxy.<name>.*} is used.
     * If set but no proxy configuration with that name is found, an exception is thrown at runtime.
     */
    Optional<String> proxyConfigurationName();

    /**
     * Set a list of remote hosts that are not proxied when the client is configured to use a proxy.
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @Deprecated
    Optional<List<String>> nonProxyHosts();

    /**
     * Set proxy options for connections via CONNECT proxy
     *
     * @deprecated use {@code quarkus.redis.tcp.proxy-configuration-name} and {@code quarkus.proxy.*}
     */
    @Deprecated
    ProxyConfig proxyOptions();

    /**
     * Set the read idle timeout.
     */
    Optional<Duration> readIdleTimeout();

    /**
     * Set the TCP receive buffer size.
     */
    OptionalInt receiveBufferSize();

    /**
     * Set the value of reconnect attempts.
     */
    OptionalInt reconnectAttempts();

    /**
     * Set the reconnect interval.
     */
    Optional<Duration> reconnectInterval();

    /**
     * Whether to reuse the address.
     */
    Optional<Boolean> reuseAddress();

    /**
     * Whether to reuse the port.
     */
    Optional<Boolean> reusePort();

    /**
     * Set the TCP send buffer size.
     */
    OptionalInt sendBufferSize();

    /**
     * Set the {@code SO_linger} keep alive duration.
     */
    Optional<Duration> soLinger();

    /**
     * Enable the {@code TCP_CORK} option - only with linux native transport.
     */
    Optional<Boolean> cork();

    /**
     * Enable the {@code TCP_FASTOPEN} option - only with linux native transport.
     */
    Optional<Boolean> fastOpen();

    /**
     * Set whether keep alive is enabled
     */
    Optional<Boolean> keepAlive();

    /**
     * Set whether no delay is enabled
     */
    Optional<Boolean> noDelay();

    /**
     * Enable the {@code TCP_QUICKACK} option - only with linux native transport.
     */
    Optional<Boolean> quickAck();

    /**
     * Set the value of traffic class.
     */
    OptionalInt trafficClass();

    /**
     * Set the write idle timeout.
     */
    Optional<Duration> writeIdleTimeout();

    /**
     * Set the local interface to bind for network connections.
     * When the local address is null, it will pick any local address, the default local address is null.
     */
    Optional<String> localAddress();
}
