package io.quarkus.reactive.datasource.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceReactiveRuntimeConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @WithDefault("false")
    boolean cachePreparedStatements();

    /**
     * The datasource URLs.
     * <p>
     * If multiple values are set, this datasource will create a pool with a list of servers instead of a single server.
     * The pool uses round-robin load balancing for server selection during connection establishment.
     * Note that certain drivers might not accommodate multiple values in this context.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> url();

    /**
     * The datasource pool maximum size.
     */
    @WithDefault("20")
    int maxSize();

    /**
     * When a new connection object is created, the pool assigns it an event loop.
     * <p>
     * When {@code #event-loop-size} is set to a strictly positive value, the pool assigns as many event loops as specified, in
     * a round-robin fashion.
     * By default, the number of event loops configured or calculated by Quarkus is used.
     * If {@code #event-loop-size} is set to zero or a negative value, the pool assigns the current event loop to the new
     * connection.
     */
    OptionalInt eventLoopSize();

    /**
     * Whether all server certificates should be trusted.
     */
    @WithDefault("false")
    boolean trustAll();

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pfx} must be disabled.
     */
    PemTrustCertConfiguration trustCertificatePem();

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@code #trust-certificate-pem} and {@code #trust-certificate-pfx} must be disabled.
     */
    JksConfiguration trustCertificateJks();

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pem} must be disabled.
     */
    PfxConfiguration trustCertificatePfx();

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pfx} must be disabled.
     */
    PemKeyCertConfiguration keyCertificatePem();

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@code #key-certificate-pem} and {@code #key-certificate-pfx} must be disabled.
     */
    JksConfiguration keyCertificateJks();

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pem} must be disabled.
     */
    PfxConfiguration keyCertificatePfx();

    /**
     * The number of reconnection attempts when a pooled connection cannot be established on first try.
     */
    @WithDefault("0")
    int reconnectAttempts();

    /**
     * The interval between reconnection attempts when a pooled connection cannot be established on first try.
     */
    @WithDefault("PT1S")
    Duration reconnectInterval();

    /**
     * The hostname verification algorithm to use in case the server's identity should be checked.
     * Should be {@code HTTPS}, {@code LDAPS} or {@code NONE}.
     * {@code NONE} is the default value and disables the verification.
     */
    @WithDefault("NONE")
    String hostnameVerificationAlgorithm();

    /**
     * The maximum time a connection remains unused in the pool before it is closed.
     */
    @ConfigDocDefault("no timeout")
    Optional<Duration> idleTimeout();

    /**
     * The maximum time a connection remains in the pool, after which it will be closed
     * upon return and replaced as necessary.
     */
    @ConfigDocDefault("no timeout")
    Optional<Duration> maxLifetime();

    /**
     * Set to true to share the pool among datasources.
     * There can be multiple shared pools distinguished by <name>name</name>, when no specific name is set,
     * the <code>__vertx.DEFAULT</code> name is used.
     */
    @WithDefault("false")
    boolean shared();

    /**
     * Set the pool name, used when the pool is shared among datasources, otherwise ignored.
     */
    Optional<String> name();

    /**
     * Other unspecified properties to be passed through the Reactive SQL Client directly to the database when new connections
     * are initiated.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> additionalProperties();
}
