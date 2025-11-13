package io.quarkus.mongodb.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.mongodb")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MongoConfig {
    String CONFIG_NAME = "mongodb";
    String DEFAULT_CLIENT_NAME = "<default>";

    /**
     * Configures the Mongo clients.
     * <p>
     * The default client does not have a name, and it is configured as:
     *
     * <pre>
     * quarkus.mongodb.connection-string = mongodb://mongo1:27017
     * </pre>
     *
     * And then use {@link jakarta.inject.Inject} to inject the client:
     *
     * <pre>
     * &#64;Inject
     * MongoClient mongoClient;
     * </pre>
     *
     * <p>
     * Named clusters must be identified to select the right client:
     *
     * <pre>
     * quarkus.mongodb.cluster1.connection-string = mongodb://mongo1:27017
     * quarkus.mongodb.cluster2.connection-string = mongodb://mongo2:27017,mongodb://mongo3:27017
     * </pre>
     *
     * And then use the {@link io.quarkus.mongodb.MongoClientName} annotation to select any of the beans:
     * <ul>
     * <li>{@link com.mongodb.client.MongoClient}</li>
     * <li>{@link io.quarkus.mongodb.reactive.ReactiveMongoClient}</li>
     * </ul>
     * And inject the client:
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
    @WithDefaults
    @WithUnnamedKey(DEFAULT_CLIENT_NAME)
    @ConfigDocMapKey("mongo-client-name")
    Map<String, MongoClientConfig> clients();

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
     * This property configures the DNS server. If the server is not set, it tries to read the first {@code nameserver} from
     * {@code /etc /resolv.conf} (if the file exists), otherwise fallback to the default.
     */
    @WithName("dns.server-host")
    Optional<String> dnsServer();

    /**
     * This property configures the DNS server port.
     */
    @WithName("dns.server-port")
    OptionalInt dnsServerPort();

    /**
     * If {@code native.dns.use-vertx-dns-resolver} is set to {@code true}, this property configures the DNS lookup timeout
     * duration.
     */
    @WithName("dns.lookup-timeout")
    @WithDefault("5s")
    Duration dnsLookupTimeout();

    /**
     * This property enables the logging ot the DNS lookup. It can be useful to understand why the lookup fails.
     */
    @WithDefault("false")
    @WithName("dns.log-activity")
    boolean dnsLookupLogActivity();

    static boolean isDefaultClient(final String name) {
        return DEFAULT_CLIENT_NAME.equalsIgnoreCase(name);
    }

    static String getPropertyName(final String name, final String attribute) {
        String prefix = DEFAULT_CLIENT_NAME.equals(name)
                ? "quarkus.mongodb."
                : "quarkus.mongodb." + (name.contains(".") ? "\"" + name + "\"" : name) + ".";
        return prefix + attribute;
    }
}
