package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = MongodbConfig.CONFIG_NAME, phase = ConfigPhase.RUN_TIME)
public class MongodbConfig {
    public static final String CONFIG_NAME = "mongodb";
    @Deprecated
    public static final String NATIVE_DNS_LOG_ACTIVITY = "native.dns.log-activity";
    public static final String DNS_LOG_ACTIVITY = "dns.log-activity";
    @Deprecated
    public static final String NATIVE_DNS_SERVER_HOST = "native.dns.server-host";
    public static final String DNS_SERVER_HOST = "dns.server-host";
    @Deprecated
    public static final String NATIVE_DNS_SERVER_PORT = "native.dns.server-port";
    public static final String DNS_SERVER_PORT = "dns.server-port";
    @Deprecated
    public static final String NATIVE_DNS_LOOKUP_TIMEOUT = "native.dns.lookup-timeout";
    public static final String DNS_LOOKUP_TIMEOUT = "dns.lookup-timeout";

    /**
     * The default mongo client connection.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public MongoClientConfig defaultMongoClientConfig;

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
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, MongoClientConfig> mongoClientConfigs;

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
    @ConfigItem(name = "native.dns.use-vertx-dns-resolver", defaultValue = "false")
    public boolean useVertxDnsResolverInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server.
     * If the server is not set, it tries to read the first {@code nameserver} from {@code /etc/resolv.conf} (if the
     * file exists), otherwise fallback to the default.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsServer}
     */
    @Deprecated
    @ConfigItem(name = NATIVE_DNS_SERVER_HOST)
    public Optional<String> dnsServerInNativeMode;

    /**
     * This property configures the DNS server. If the server is not set, it tries to read the first {@code nameserver} from
     * {@code /etc /resolv.conf} (if the file exists), otherwise fallback to the default.
     */
    @ConfigItem(name = DNS_SERVER_HOST)
    public Optional<String> dnsServer;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server port.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsServerPort}
     */
    @Deprecated
    @ConfigItem(name = NATIVE_DNS_SERVER_PORT)
    public OptionalInt dnsServerPortInNativeMode;
    /**
     * This property configures the DNS server port.
     */
    @ConfigItem(name = DNS_SERVER_PORT)
    public OptionalInt dnsServerPort;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsLookupTimeout}
     */
    @Deprecated
    @ConfigItem(name = NATIVE_DNS_LOOKUP_TIMEOUT, defaultValue = "5s")
    public Duration dnsLookupTimeoutInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     */
    @ConfigItem(name = DNS_LOOKUP_TIMEOUT, defaultValue = "5s")
    public Duration dnsLookupTimeout;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property enables the logging ot the
     * DNS lookup. It can be useful to understand why the lookup fails.
     *
     * @deprecated this property has been deprecated in favor of {@link #dnsLookupLogActivity}
     */
    @Deprecated
    @ConfigItem(name = NATIVE_DNS_LOG_ACTIVITY, defaultValue = "false")
    public Optional<Boolean> dnsLookupLogActivityInNativeMode;

    /**
     * This property enables the logging ot the DNS lookup. It can be useful to understand why the lookup fails.
     */
    @ConfigItem(name = DNS_LOG_ACTIVITY, defaultValue = "false")
    public Optional<Boolean> dnsLookupLogActivity;
}
