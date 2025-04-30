package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.bson.UuidRepresentation;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface MongoClientConfig {

    /**
     * Configures the connection string.
     * The format is:
     * {@code  mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]}
     * <p>
     * {@code mongodb://} is a required prefix to identify that this is a string in the standard connection format.
     * <p>
     * {@code username:password@} are optional. If given, the driver will attempt to log in to a database after
     * connecting to a database server. For some authentication mechanisms, only the username is specified and the
     * password is not, in which case the ":" after the username is left off as well.
     * <p>
     * {@code host1} is the only required part of the connection string. It identifies a server address to connect to.
     * <p>
     * {@code :portX} is optional and defaults to :27017 if not provided.
     * <p>
     * {@code /database} is the name of the database to log in to and thus is only relevant if the
     * {@code username:password@} syntax is used. If not specified the {@code admin} database will be used by default.
     * <p>
     * {@code ?options} are connection options. Note that if {@code database} is absent there is still a {@code /}
     * required between the last host and the {@code ?} introducing the options. Options are name=value pairs and the
     * pairs are separated by "&amp;".
     * <p>
     * An alternative format, using the {@code mongodb+srv} protocol, is:
     *
     * <pre>
     *  mongodb+srv://[username:password@]host[/[database][?options]]
     * </pre>
     * <ul>
     * <li>{@code mongodb+srv://} is a required prefix for this format.</li>
     * <li>{@code username:password@} are optional. If given, the driver will attempt to login to a database after
     * connecting to a database server. For some authentication mechanisms, only the username is specified and the
     * password is not, in which case the ":" after the username is left off as well</li>
     * <li>{@code host} is the only required part of the URI. It identifies a single host name for which SRV records
     * are looked up from a Domain Name Server after prefixing the host name with {@code "_mongodb._tcp"}. The
     * host/port for each SRV record becomes the seed list used to connect, as if each one were provided as host/port
     * pair in a URI using the normal mongodb protocol.</li>
     * <li>{@code /database} is the name of the database to login to and thus is only relevant if the
     * {@code username:password@} syntax is used. If not specified the "admin" database will be used by default.</li>
     * <li>{@code ?options} are connection options. Note that if {@code database} is absent there is still a {@code /}
     * required between the last host and the {@code ?} introducing the options. Options are name=value pairs and the
     * pairs are separated by "&amp;". Additionally with the mongodb+srv protocol, TXT records are looked up from a
     * Domain Name Server for the given host, and the text value of each one is prepended to any options on the URI
     * itself. Because the last specified value for any option wins, that means that options provided on the URI will
     * override any that are provided via TXT records.</li>
     * </ul>
     */
    Optional<String> connectionString();

    /**
     * Configures the MongoDB server addresses (one if single mode).
     * The addresses are passed as {@code host:port}.
     */
    @WithDefault("127.0.0.1:27017")
    List<String> hosts();

    /**
     * Configure the database name.
     */
    Optional<String> database();

    /**
     * Configures the application name.
     */
    Optional<String> applicationName();

    /**
     * Configures the maximum number of connections in the connection pool.
     */
    OptionalInt maxPoolSize();

    /**
     * Configures the minimum number of connections in the connection pool.
     */
    OptionalInt minPoolSize();

    /**
     * Maximum idle time of a pooled connection. A connection that exceeds this limit will be closed.
     */
    Optional<Duration> maxConnectionIdleTime();

    /**
     * Maximum lifetime of a pooled connection. A connection that exceeds this limit will be closed.
     */
    Optional<Duration> maxConnectionLifeTime();

    /**
     * Configures the time period between runs of the maintenance job.
     */
    Optional<Duration> maintenanceFrequency();

    /**
     * Configures period of time to wait before running the first maintenance job on the connection pool.
     */
    Optional<Duration> maintenanceInitialDelay();

    /**
     * How long a connection can take to be opened before timing out.
     */
    Optional<Duration> connectTimeout();

    /**
     * How long a socket read can take before timing out.
     */
    Optional<Duration> readTimeout();

    /**
     * If connecting with TLS, this option enables insecure TLS connections.
     *
     * @deprecated in favor of configuration at the tls registry level. See {@link #tlsConfigurationName()}
     *             and quarkus tls registry hostname verification configuration
     *             {@code quarkus.tls.hostname-verification-algorithm=NONE}.
     */
    @WithDefault("false")
    @Deprecated(forRemoval = true, since = "3.21")
    boolean tlsInsecure();

    /**
     * Whether to connect using TLS.
     */
    @WithDefault("false")
    boolean tls();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * The default TLS configuration is <strong>not</strong> used by default.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Implies that the hosts given are a seed list, and the driver will attempt to find all members of the set.
     */
    Optional<String> replicaSetName();

    /**
     * How long the driver will wait for server selection to succeed before throwing an exception.
     */
    Optional<Duration> serverSelectionTimeout();

    /**
     * When choosing among multiple MongoDB servers to send a request, the driver will only send that request to a
     * server whose ping time is less than or equal to the server with the fastest ping time plus the local threshold.
     */
    Optional<Duration> localThreshold();

    /**
     * The frequency that the driver will attempt to determine the current state of each server in the cluster.
     */
    Optional<Duration> heartbeatFrequency();

    /**
     * Write concern
     */
    @ConfigDocSection
    WriteConcernConfig writeConcern();

    /**
     * Configures the read concern.
     * Supported values are: {@code local|majority|linearizable|snapshot|available}
     */
    Optional<String> readConcern();

    /**
     * Configures the read preference.
     * Supported values are: {@code primary|primaryPreferred|secondary|secondaryPreferred|nearest}
     */
    Optional<String> readPreference();

    /**
     * Credentials and authentication mechanism
     */
    @ConfigDocSection
    CredentialConfig credentials();

    /**
     * The database used during the readiness health checks
     */
    @WithName("health.database")
    @WithDefault("admin")
    String healthDatabase();

    /**
     * Configures the UUID representation to use when encoding instances of {@link java.util.UUID}
     * and when decoding BSON binary values with subtype of 3.
     */
    Optional<UuidRepresentation> uuidRepresentation();

    enum ReactiveTransportConfig {
        /**
         * Uses a Netty-based transport re-using the existing Netty event loops.
         */
        NETTY,
        /**
         * With a reactive driver it uses an async transport backed by a driver-managed thread pool.
         */
        MONGO
    }

    /**
     * Configures the reactive transport.
     */
    @WithDefault("netty")
    ReactiveTransportConfig reactiveTransport();

}
