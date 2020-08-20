package io.quarkus.reactive.datasource.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;

/**
 * For now, the reactive extensions only support a default datasource.
 */
@ConfigRoot(name = "datasource.reactive", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactiveRuntimeConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @ConfigItem(defaultValue = "false")
    public boolean cachePreparedStatements;

    /**
     * The datasource URL.
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The datasource pool maximum size.
     */
    @ConfigItem
    public OptionalInt maxSize;

    /**
     * Whether all server certificates should be trusted.
     */
    @ConfigItem(defaultValue = "false")
    public boolean trustAll;

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustCertificatePem;

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@code #trust-certificate-pem} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration trustCertificateJks;

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration trustCertificatePfx;

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertificatePem;

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@code #key-certificate-pem} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration keyCertificateJks;

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration keyCertificatePfx;

    /**
     * Experimental: use one connection pool per thread.
     */
    @ConfigItem
    public Optional<Boolean> threadLocal;

    /**
     * The number of reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "0")
    public int reconnectAttempts;

    /**
     * The interval between reconnection attempts when a pooled connection cannot be established on first try.
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration reconnectInterval;

    /**
     * The maximum time without data written to or read from a connection before it is removed from the pool.
     */
    @ConfigItem(defaultValueDocumentation = "no timeout")
    public Optional<Duration> idleTimeout;
}
