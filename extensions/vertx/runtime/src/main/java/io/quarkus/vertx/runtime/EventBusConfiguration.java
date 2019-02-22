package io.quarkus.vertx.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class EventBusConfiguration {

    /**
     * The key configuration for the PEM format.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertificatePem;

    /**
     * The key configuration for the JKS format.
     */
    @ConfigItem
    public JksConfiguration keyCertificateJks;

    /**
     * The key configuration for the PFX format.
     */
    @ConfigItem
    public PfxConfiguration keyCertificatePfx;

    /**
     * The trust key configuration for the PEM format.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustCertificatePem;

    /**
     * The trust key configuration for the JKS format.
     */
    @ConfigItem
    public JksConfiguration trustCertificateJks;

    /**
     * The trust key configuration for the PFX format.
     */
    @ConfigItem
    public PfxConfiguration trustCertificatePfx;

    /**
     * The accept backlog.
     */
    @ConfigItem
    public OptionalInt acceptBacklog;

    /**
     * The client authentication.
     */
    @ConfigItem(defaultValue = "NONE")
    public String clientAuth;

    /**
     * The connect timeout.
     */
    @ConfigItem(defaultValue = "PT60S")
    public Duration connectTimeout;

    /**
     * The idle timeout in milliseconds.
     */
    @ConfigItem
    public Optional<Duration> idleTimeout;

    /**
     * The receive buffer size.
     */
    @ConfigItem
    public OptionalInt receiveBufferSize;

    /**
     * The number of reconnection attempts.
     */
    @ConfigItem
    public int reconnectAttempts;

    /**
     * The reconnection interval in milliseconds.
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration reconnectInterval;

    /**
     * Whether or not to reuse the address.
     */
    @ConfigItem(defaultValue = "true")
    public boolean reuseAddress;

    /**
     * Whether or not to reuse the port.
     */
    @ConfigItem
    public boolean reusePort;

    /**
     * The send buffer size.
     */
    @ConfigItem
    public OptionalInt sendBufferSize;

    /**
     * The so linger.
     */
    @ConfigItem(name = "soLinger")
    public OptionalInt soLinger;

    /**
     * Enables or Disabled SSL.
     */
    @ConfigItem
    public boolean ssl;

    /**
     * Whether or not to keep the TCP connection opened (keep-alive).
     */
    @ConfigItem
    public boolean tcpKeepAlive;

    /**
     * Configure the TCP no delay.
     */
    @ConfigItem(defaultValue = "true")
    public boolean tcpNoDelay;

    /**
     * Configure the traffic class.
     */
    @ConfigItem
    public OptionalInt trafficClass;

    /**
     * Enables or disables the trust all parameter.
     */
    @ConfigItem
    public boolean trustAll;

}
