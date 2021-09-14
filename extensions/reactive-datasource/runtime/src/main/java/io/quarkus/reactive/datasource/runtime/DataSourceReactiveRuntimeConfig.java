package io.quarkus.reactive.datasource.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;

@ConfigGroup
public class DataSourceReactiveRuntimeConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @ConfigItem(defaultValue = "false")
    public boolean cachePreparedStatements = false;

    /**
     * The datasource URL.
     */
    @ConfigItem
    public Optional<String> url = Optional.empty();

    /**
     * The datasource pool maximum size.
     */
    @ConfigItem
    public OptionalInt maxSize = OptionalInt.empty();

    /**
     * Whether all server certificates should be trusted.
     */
    @ConfigItem(defaultValue = "false")
    public boolean trustAll = false;

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustCertificatePem = new PemTrustCertConfiguration();

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@code #trust-certificate-pem} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration trustCertificateJks = new JksConfiguration();

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration trustCertificatePfx = new PfxConfiguration();

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertificatePem = new PemKeyCertConfiguration();

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@code #key-certificate-pem} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration keyCertificateJks = new JksConfiguration();

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration keyCertificatePfx = new PfxConfiguration();

    /**
     * Deprecated: this was removed and is no longer available.
     * 
     * @Deprecated
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> threadLocal = Optional.empty();

    /**
     * The number of reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "0")
    public int reconnectAttempts = 0;

    /**
     * The interval between reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration reconnectInterval = Duration.ofSeconds(1L);

    /**
     * The hostname verification algorithm to use in case the server's identity should be checked.
     * Should be HTTPS, LDAPS or an empty string.
     */
    @ConfigItem
    public Optional<String> hostnameVerificationAlgorithm = Optional.empty();

    /**
     * The maximum time a connection remains unused in the pool before it is closed.
     */
    @ConfigItem(defaultValueDocumentation = "no timeout")
    public Optional<Duration> idleTimeout = Optional.empty();
}
