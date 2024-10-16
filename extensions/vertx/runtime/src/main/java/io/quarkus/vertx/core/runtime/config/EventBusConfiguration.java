package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface EventBusConfiguration {

    /**
     * The key configuration for the PEM format.
     */
    PemKeyCertConfiguration keyCertificatePem();

    /**
     * The key configuration for the JKS format.
     */
    JksConfiguration keyCertificateJks();

    /**
     * The key configuration for the PFX format.
     */
    PfxConfiguration keyCertificatePfx();

    /**
     * The trust key configuration for the PEM format.
     */
    PemTrustCertConfiguration trustCertificatePem();

    /**
     * The trust key configuration for the JKS format.
     */
    JksConfiguration trustCertificateJks();

    /**
     * The trust key configuration for the PFX format.
     */
    PfxConfiguration trustCertificatePfx();

    /**
     * The accept backlog.
     */
    OptionalInt acceptBacklog();

    /**
     * The client authentication.
     */
    @WithDefault("NONE")
    String clientAuth();

    /**
     * The connect timeout.
     */
    @WithDefault("60")
    Duration connectTimeout();

    /**
     * The idle timeout in milliseconds.
     */
    Optional<Duration> idleTimeout();

    /**
     * The receive buffer size.
     */
    OptionalInt receiveBufferSize();

    /**
     * The number of reconnection attempts.
     */
    @WithDefault("0")
    int reconnectAttempts();

    /**
     * The reconnection interval in milliseconds.
     */
    @WithDefault("1")
    Duration reconnectInterval();

    /**
     * Whether to reuse the address.
     */
    @WithDefault("true")
    boolean reuseAddress();

    /**
     * Whether to reuse the port.
     */
    @WithDefault("false")
    boolean reusePort();

    /**
     * The send buffer size.
     */
    OptionalInt sendBufferSize();

    /**
     * The so linger.
     */
    OptionalInt soLinger();

    /**
     * Enables or Disabled SSL.
     */
    @WithDefault("false")
    boolean ssl();

    /**
     * Whether to keep the TCP connection opened (keep-alive).
     */
    @WithDefault("false")
    boolean tcpKeepAlive();

    /**
     * Configure the TCP no delay.
     */
    @WithDefault("true")
    boolean tcpNoDelay();

    /**
     * Configure the traffic class.
     */
    OptionalInt trafficClass();

    /**
     * Enables or disables the trust all parameter.
     */
    @WithDefault("false")
    boolean trustAll();

}
