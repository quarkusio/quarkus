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
    @ConfigItem
    public Optional<SslMode> sslMode;

    /**
     * Trust configuration in the PEM format.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustPem;

    /**
     * Trust configuration in the JKS format.
     */
    @ConfigItem
    public JksConfiguration trustJks;

    /**
     * Trust configuration in the PFX format.
     */
    @ConfigItem
    public PfxConfiguration trustPfx;

    /**
     * Key/cert configuration in the PEM format.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertPem;

    /**
     * Key/cert configuration in the JKS format.
     */
    @ConfigItem
    public JksConfiguration keyCertJks;

    /**
     * Key/cert configuration in the PFX format.
     */
    @ConfigItem
    public PfxConfiguration keyCertPfx;
}
