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
     * <strong>IMPORTANT:</strong> The resolution may be different in JVM mode (using the default (JNDI-based) DNS resolver,
     * and in native mode. This feature is experimental.
     */
    @ConfigItem(name = "native.dns.use-vertx-dns-resolver", defaultValue = "false")
    public boolean useVertxDnsResolverInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server.
     * If the server is not set, it tries to read the first {@code nameserver} from {@code /etc/resolv.conf} (if the
     * file exists), otherwise fallback to the default.
     */
    @ConfigItem(name = "native.dns.server-host")
    public Optional<String> dnsServerInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS server port.
     * If not set, uses the system DNS resolver.
     */
    @ConfigItem(name = "native.dns.server-port", defaultValue = "53")
    public OptionalInt dnsServerPortInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     */
    @ConfigItem(name = "native.dns.lookup-timeout", defaultValue = "5s")
    public Duration dnsLookupTimeoutInNativeMode;

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property enables the logging ot the
     * DNS lookup. It can be useful to understand why the lookup fails.
     */
    @ConfigItem(name = "native.dns.log-activity", defaultValue = "false")
    public Optional<Boolean> dnsLookupLogActivityInNativeMode;
}
