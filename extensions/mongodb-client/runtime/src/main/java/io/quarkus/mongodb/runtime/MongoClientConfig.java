package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mongodb", phase = ConfigPhase.RUN_TIME)
public class MongoClientConfig {

    /**
     * Configures the connection string.
     * The format is:
     * {@code  mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]}
     * <p>
     * {@code mongodb://} is a required prefix to identify that this is a string in the standard connection format.
     * <p>
     * {@code username:password@} are optional. If given, the driver will attempt to login to a database after
     * connecting to a database server. For some authentication mechanisms, only the username is specified and the
     * password is not, in which case the ":" after the username is left off as well.
     * <p>
     * {@code host1} is the only required part of the connection string. It identifies a server address to connect to.
     * <p>
     * {@code :portX} is optional and defaults to :27017 if not provided.
     * <p>
     * {@code /database} is the name of the database to login to and thus is only relevant if the
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
    @ConfigItem
    public Optional<String> connectionString;

    /**
     * Configures the Mongo server addressed (one if single mode).
     * The addressed are passed as {@code host:port}.
     */
    @ConfigItem
    public List<String> hosts;

    /**
     * Configure the database name.
     */
    @ConfigItem
    public Optional<String> database;

    /**
     * Configures the application name.
     */
    @ConfigItem
    public Optional<String> applicationName;

    /**
     * Configures the maximum number of connections in the connection pool.
     */
    @ConfigItem
    public OptionalInt maxPoolSize;

    /**
     * Configures the minimum number of connections in the connection pool.
     */
    @ConfigItem
    public OptionalInt minPoolSize;

    /**
     * Maximum idle time of a pooled connection. A connection that exceeds this limit will be closed.
     */
    @ConfigItem
    public Optional<Duration> maxConnectionIdleTime;

    /**
     * Maximum life time of a pooled connection. A connection that exceeds this limit will be closed.
     */
    @ConfigItem
    public Optional<Duration> maxConnectionLifeTime;

    /**
     * The maximum wait time that a thread may wait for a connection to become available.
     */
    @ConfigItem
    public Optional<Duration> waitQueueTimeout;

    /**
     * Configures the time period between runs of the maintenance job.
     */
    @ConfigItem
    public Optional<Duration> maintenanceFrequency;

    /**
     * Configures period of time to wait before running the first maintenance job on the connection pool.
     */
    @ConfigItem
    public Optional<Duration> maintenanceInitialDelay;

    /**
     * This multiplier, multiplied with the {@code maxPoolSize} setting, gives the maximum number of
     * threads that may be waiting for a connection to become available from the pool. All further threads will get an
     * exception right away.
     */
    @ConfigItem
    public OptionalInt waitQueueMultiple;

    /**
     * How long a connection can take to be opened before timing out.
     */
    @ConfigItem
    public Optional<Duration> connectTimeout;

    /**
     * How long a send or receive on a socket can take before timing out.
     */
    @ConfigItem
    public Optional<Duration> socketTimeout;

    /**
     * If connecting with TLS, this option enables insecure TLS connections.
     */
    @ConfigItem(defaultValue = "false")
    public boolean tlsInsecure;

    /**
     * Whether to connect using TLS.
     */
    @ConfigItem(defaultValue = "false")
    public boolean tls;

    /**
     * Implies that the hosts given are a seed list, and the driver will attempt to find all members of the set.
     */
    @ConfigItem
    public Optional<String> replicaSetName;

    /**
     * How long the driver will wait for server selection to succeed before throwing an exception.
     */
    @ConfigItem
    public Optional<Duration> serverSelectionTimeout;

    /**
     * When choosing among multiple MongoDB servers to send a request, the driver will only send that request to a
     * server whose ping time is less than or equal to the server with the fastest ping time plus the local threshold.
     */
    @ConfigItem
    public Optional<Duration> localThreshold;

    /**
     * The frequency that the driver will attempt to determine the current state of each server in the cluster.
     */
    @ConfigItem
    public Optional<Duration> heartbeatFrequency;

    /**
     * Configures the write concern.
     */
    public WriteConcernConfig writeConcern;

    /**
     * Configures the read preferences.
     * Supported values are: {@code primary|primaryPreferred|secondary|secondaryPreferred|nearest}
     */
    @ConfigItem
    public Optional<String> readPreference;

    /**
     * Configures the credentials and the authentication mechanism.
     */
    public CredentialConfig credentials;

    /**
     * Configures the maximum number of concurrent operations allowed to wait for a server to become available.
     * All further operations will get an exception immediately.
     */
    @ConfigItem
    public OptionalInt maxWaitQueueSize;

}
