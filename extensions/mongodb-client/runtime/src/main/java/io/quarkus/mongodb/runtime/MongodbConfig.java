package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.mongodb")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MongodbConfig {
    String CONFIG_NAME = "mongodb";
    @Deprecated
    String NATIVE_DNS_LOG_ACTIVITY = "native.dns.log-activity";
    String DNS_LOG_ACTIVITY = "dns.log-activity";
    @Deprecated
    String NATIVE_DNS_SERVER_HOST = "native.dns.server-host";
    String DNS_SERVER_HOST = "dns.server-host";
    @Deprecated
    String NATIVE_DNS_SERVER_PORT = "native.dns.server-port";
    String DNS_SERVER_PORT = "dns.server-port";
    @Deprecated
    String NATIVE_DNS_LOOKUP_TIMEOUT = "native.dns.lookup-timeout";
    String DNS_LOOKUP_TIMEOUT = "dns.lookup-timeout";

    /**
     * The default mongo client connection.
     */
    @WithParentName
    MongoClientConfig defaultMongoClientConfig();

    /**
     * Configures additional mongo client connections.
     * <p>
     * each cluster have a unique identifier witch must be identified to select the right connection.
     * example:
     * <p>
     *
     * <pre>
     * quarkus.mongodb.cluster1.connection-string = mongodb://mongo1:27017
     * quarkus.mongodb.cluster2.connection-string = mongodb://mongo2:27017,mongodb://mongo3:27017
     * </pre>
     * <p>
     * And then use annotations above the instances of MongoClient to indicate which instance we are going to use
     * <p>
     *
     * <pre>
     * {@code
     * &#64;MongoClientName("cluster1")
     * &#64;Inject
     * ReactiveMongoClient mongoClientCluster1
     * }
     * </pre>
     */
    @WithParentName
    Map<String, MongoClientConfig> mongoClientConfigs();

    /**
     * The default DNS resolver used to handle {@code mongo+srv://} urls cannot be used in a native executable.
     * This option enables a fallback to use Vert.x to resolve the server names instead of JNDI.
     *
     * <strong>IMPORTANT:</strong> The resolution may be different in JVM mode using the default (JNDI-based) DNS resolver,
     * and in native mode. This feature is experimental.
     *
     * @deprecated This resolver is always used
     */
    @Deprecated
    @WithName("native.dns.use-vertx-dns-resolver")
    @WithDefault("false")
    boolean useVertxDnsResolverInNativeMode();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server.
     * If the server is not set, it tries to read the first {@code nameserver} from {@code /etc/resolv.conf} (if the
     * file exists), otherwise fallback to the default.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsServer}
     */
    @Deprecated
    @WithName(NATIVE_DNS_SERVER_HOST)
    Optional<String> dnsServerInNativeMode();

    /**
     * This property configures the DNS server. If the server is not set, it tries to read the first {@code nameserver} from
     * {@code /etc /resolv.conf} (if the file exists), otherwise fallback to the default.
     */
    @WithName(DNS_SERVER_HOST)
    Optional<String> dnsServer();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server port.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsServerPort}
     */
    @Deprecated
    @WithName(NATIVE_DNS_SERVER_PORT)
    OptionalInt dnsServerPortInNativeMode();

    /**
     * This property configures the DNS server port.
     */
    @WithName(DNS_SERVER_PORT)
    OptionalInt dnsServerPort();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsLookupTimeout}
     */
    @Deprecated
    @WithName(NATIVE_DNS_LOOKUP_TIMEOUT)
    @WithDefault("5s")
    Duration dnsLookupTimeoutInNativeMode();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     */
    @WithName(DNS_LOOKUP_TIMEOUT)
    @WithDefault("5s")
    Duration dnsLookupTimeout();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property enables the logging ot the
     * DNS lookup. It can be useful to understand why the lookup fails.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsLookupLogActivity}
     */
    @Deprecated
    @WithDefault("false")
    @WithName(NATIVE_DNS_LOG_ACTIVITY)
    Optional<Boolean> dnsLookupLogActivityInNativeMode();

    /**
     * This property enables the logging ot the DNS lookup. It can be useful to understand why the lookup fails.
     */
    @WithDefault("false")
    @WithName(DNS_LOG_ACTIVITY)
    Optional<Boolean> dnsLookupLogActivity();
}
