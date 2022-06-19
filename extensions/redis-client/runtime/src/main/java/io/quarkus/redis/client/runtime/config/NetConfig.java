package io.quarkus.redis.client.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class NetConfig {

    /**
     * Set the ALPN usage.
     */
    @ConfigItem
    public Optional<Boolean> alpn;

    /**
     * Sets the list of application-layer protocols to provide to the server during the
     * {@code Application-Layer Protocol Negotiation}.
     */
    @ConfigItem
    public Optional<List<String>> applicationLayerProtocols;

    /**
     * Sets the list of enabled SSL/TLS protocols.
     */
    @ConfigItem
    public Optional<Set<String>> secureTransportProtocols;

    /**
     * Set the idle timeout.
     */
    @ConfigItem
    public Optional<Duration> idleTimeout;

    /**
     * Set the connect timeout.
     */
    @ConfigItem
    public Optional<Duration> connectionTimeout;

    /**
     * Set a list of remote hosts that are not proxied when the client is configured to use a proxy.
     */
    @ConfigItem
    public Optional<List<String>> nonProxyHosts;

    /**
     * Set proxy options for connections via CONNECT proxy
     */
    @ConfigItem
    public Optional<ProxyConfig> proxyOptions;

    /**
     * Set the read idle timeout.
     */
    @ConfigItem
    public Optional<Duration> readIdleTimeout;

    /**
     * Set the TCP receive buffer size.
     */
    @ConfigItem
    public OptionalInt receiveBufferSize;

    /**
     * Set the value of reconnect attempts.
     */
    @ConfigItem
    public OptionalInt reconnectAttempts;

    /**
     * Set the reconnect interval.
     */
    @ConfigItem
    public Optional<Duration> reconnectInterval;

    /**
     * Whether to reuse the address.
     */
    @ConfigItem
    public Optional<Boolean> reuseAddress;

    /**
     * Whether to reuse the port.
     */
    @ConfigItem
    public Optional<Boolean> reusePort;

    /**
     * Set the TCP send buffer size.
     */
    @ConfigItem
    public OptionalInt sendBufferSize;

    /**
     * Set the {@code SO_linger} keep alive duration.
     */
    @ConfigItem
    public Optional<Duration> soLinger;

    /**
     * Enable the {@code TCP_CORK} option - only with linux native transport.
     */
    @ConfigItem
    public Optional<Boolean> cork;

    /**
     * Enable the {@code TCP_FASTOPEN} option - only with linux native transport.
     */
    @ConfigItem
    public Optional<Boolean> fastOpen;

    /**
     * Set whether keep alive is enabled
     */
    @ConfigItem
    public Optional<Boolean> keepAlive;

    /**
     * Set whether no delay is enabled
     */
    @ConfigItem
    public Optional<Boolean> noDelay;

    /**
     * Enable the {@code TCP_QUICKACK} option - only with linux native transport.
     */
    @ConfigItem
    public Optional<Boolean> quickAck;

    /**
     * Set the value of traffic class.
     */
    @ConfigItem
    public OptionalInt trafficClass;

    /**
     * Set the write idle timeout.
     */
    @ConfigItem
    public Optional<Duration> writeIdleTimeout;

    /**
     * Set the local interface to bind for network connections.
     * When the local address is null, it will pick any local address, the default local address is null.
     */
    @ConfigItem
    public Optional<String> localAddress;
}
