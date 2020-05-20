package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.vertx.pgclient.SslMode;

@ConfigRoot(name = "datasource.reactive.postgresql", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactivePostgreSQLConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @ConfigItem
    public Optional<Boolean> cachePreparedStatements;

    /**
     * The maximum number of inflight database commands that can be pipelined.
     */
    @ConfigItem
    public OptionalInt pipeliningLimit;

    /**
     * SSL operating mode of the client.
     * <p>
     * See <a href="https://www.postgresql.org/docs/current/libpq-ssl.html#LIBPQ-SSL-PROTECTION">Protection Provided in
     * Different Modes</a>.
     */
    @ConfigItem(defaultValueDocumentation = "disable")
    public Optional<SslMode> sslMode;

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@link #trustCertificateJks} and {@link #trustCertificatePfx} must be disabled.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustCertificatePem;

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@link #trustCertificatePem} and {@link #trustCertificatePfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration trustCertificateJks;

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@link #trustCertificateJks} and {@link #trustCertificatePem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration trustCertificatePfx;

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@link #keyCertificateJks} and {@link #keyCertificatePfx} must be disabled.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertificatePem;

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@link #keyCertificatePem} and {@link #keyCertificatePfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration keyCertificateJks;

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@link #keyCertificateJks} and {@link #keyCertificatePem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration keyCertificatePfx;
}
