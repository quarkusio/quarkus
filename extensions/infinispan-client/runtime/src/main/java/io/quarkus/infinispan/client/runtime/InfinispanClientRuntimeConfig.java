package io.quarkus.infinispan.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.infinispan.client.hotrod.configuration.NearCacheMode;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * @author Katia Aresti
 */
@ConfigGroup
public class InfinispanClientRuntimeConfig {

    // @formatter:off
    /**
     * Sets the URI of the running Infinispan server to connect to. hotrod://localhost:11222@admin:password
     * If provided {@link #hosts}, {@link #username} and {@link #password} will be ignored.
     */
    // @formatter:on
    @ConfigItem
    public Optional<String> uri;

    // @formatter:off
    /**
     * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).
     */
    // @formatter:on
    @ConfigItem
    public Optional<String> hosts;

    // @formatter:off
    /**
     * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).
     * @deprecated {@link #hosts} should be used to configure the list or uri for an uri connection string.
     */
    // @formatter:on
    @ConfigItem
    @Deprecated
    public Optional<String> serverList;

    // @formatter:off
    /**
     * Sets client intelligence used by authentication
     * Available values:
     * * `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list
     *              of servers supplied at configuration time.
     * * `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the
     *              remote TLS host.
     * * `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request
     *              involving keys will be routed to the server who is the primary owner which improves performance
     *              greatly. This is the default.
     */
    // @formatter:on
    @ConfigItem(defaultValue = "HASH_DISTRIBUTION_AWARE")
    Optional<String> clientIntelligence;

    // @formatter:off
    /**
     * Enables or disables authentication. Set it to false when connecting to an Infinispan Server without authentication.
     * deployments. Default is 'true'.
     */
    // @formatter:on
    @ConfigItem(defaultValue = "true")
    Optional<Boolean> useAuth;

    /**
     * Sets username used by authentication.
     */
    @ConfigItem
    Optional<String> username;

    /**
     * Sets username used by authentication.
     *
     * @deprecated {@link #username} should be used to configure the credentials username.
     */
    @ConfigItem
    @Deprecated
    Optional<String> authUsername;

    /**
     * Sets password used by authentication.
     */
    @ConfigItem
    Optional<String> password;

    /**
     * Sets password used by authentication
     *
     * @deprecated {@link #password} should be used to configure the credentials password.
     */
    @ConfigItem
    @Deprecated
    Optional<String> authPassword;

    /**
     * Sets realm used by authentication
     */
    @ConfigItem(defaultValue = "default")
    Optional<String> authRealm;

    /**
     * Sets server name used by authentication
     */
    @ConfigItem(defaultValue = "infinispan")
    Optional<String> authServerName;

    // @formatter:off
    /**
     * Sets SASL mechanism used by authentication.
     * Available values:
     * * `DIGEST-MD5` - Uses the MD5 hashing algorithm in addition to nonces to encrypt credentials. This is the default.
     * * `EXTERNAL` - Uses client certificates to provide valid identities to Infinispan Server and enable encryption.
     * * `PLAIN` - Sends credentials in plain text (unencrypted) over the wire in a way that is similar to HTTP BASIC
     *             authentication. You should use `PLAIN` authentication only in combination with TLS encryption.
     */
    // @formatter:on
    @ConfigItem(defaultValue = "DIGEST-MD5")
    Optional<String> saslMechanism;

    /**
     * Specifies the filename of a truststore to use to create the {@link SSLContext}.
     * You also need to specify a trustStorePassword.
     * Setting this property implicitly enables SSL/TLS.
     */
    @ConfigItem
    Optional<String> trustStore;

    /**
     * Specifies the password needed to open the truststore You also need to specify a trustStore.
     * Setting this property implicitly enables SSL/TLS.
     */
    @ConfigItem
    Optional<String> trustStorePassword;

    /**
     * Specifies the type of the truststore, such as JKS or JCEKS. Defaults to JKS if trustStore is enabled.
     */
    @ConfigItem
    Optional<String> trustStoreType;

    /**
     * Configures the secure socket protocol.
     * Setting this property implicitly enables SSL/TLS.
     */
    @ConfigItem
    Optional<String> sslProtocol;

    /**
     * Sets the ssl provider. For example BCFIPS
     * Setting this implicitly enables SSL/TLS.
     */
    @ConfigItem
    Optional<String> sslProvider;

    /**
     * Configures the ciphers.
     * Setting this property implicitly enables SSL/TLS.
     */
    @ConfigItem
    Optional<List<String>> sslCiphers;

    /**
     * Do SSL hostname validation.
     * Defaults to true.
     */
    @ConfigItem
    Optional<Boolean> sslHostNameValidation;

    /**
     * SNI host name. Mandatory when SSL is enabled and host name validation is true.
     */
    @ConfigItem
    Optional<String> sniHostName;

    /**
     * Whether a tracing propagation is enabled in case the Opentelemetry extension is present.
     * By default the propagation of the context is propagated from the client to the Infinispan Server.
     */
    @ConfigItem(name = "tracing.propagation.enabled")
    public Optional<Boolean> tracingPropagationEnabled;

    /**
     * Configures caches from the client with the provided configuration.
     */
    @ConfigItem
    public Map<String, InfinispanClientRuntimeConfig.RemoteCacheConfig> cache;

    /**
     * // @formatter:off
     * Configures cross site replication from the client with the provided configuration.
     * This allows automatic failover when a site is down, as well as switching manual with methods like:
     * <code>
     *    cacheManager.switchToDefaultCluster();
     *    cacheManager.switchToCluster('clusterName')
     * </code>
     * // @formatter:on
     */
    @ConfigItem
    public Map<String, InfinispanClientRuntimeConfig.BackupClusterConfig> backupCluster;

    @ConfigGroup
    public static class RemoteCacheConfig {

        // @formatter:off
        /**
         * Cache configuration in inlined XML to create the cache on first access.
         * Will be ignored if the configuration-uri is provided for the same cache name.
         * An example of the user defined property:
         * quarkus.infinispan-client.cache.bookscache.configuration=<distributed-cache><encoding media-type="application/x-protostream"/></distributed-cache>
         */
        // @formatter:on
        @ConfigItem
        public Optional<String> configuration;

        // @formatter:off
        /**
         * Cache configuration file in XML, Json or YAML whose path will be converted to URI to create the cache on first access.
         * An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder:
         * quarkus.infinispan-client.cache.bookscache.configuration-uri=cacheConfig.xml
         */
        // @formatter:on
        @ConfigItem
        public Optional<String> configurationUri;

        /**
         * The maximum number of entries to keep locally for the specified cache.
         */
        @ConfigItem
        public Optional<Integer> nearCacheMaxEntries;

        // @formatter:off
        /**
         * Sets near cache mode used by the Infinispan Client
         * Available values:
         * * `DISABLED` - Means that near caching is disabled. This is the default value.
         * * `INVALIDATED` - Means is near caching is invalidated, so when entries are updated or removed server-side,
         *                   invalidation messages will be sent to clients to remove them from the near cache.
         */
        // @formatter:on
        @ConfigItem
        public Optional<NearCacheMode> nearCacheMode;

        // @formatter:off
        /**
         * Enables bloom filter for near caching.
         * Bloom filters optimize performance for write operations by reducing the total number of
         * invalidation messages.
         */
        // @formatter:on
        @ConfigItem
        public Optional<Boolean> nearCacheUseBloomFilter;
    }

    @ConfigGroup
    public static class BackupClusterConfig {
        // @formatter:off
        /**
         * Sets the host name/port to connect to. Each one is separated by a semicolon (eg. hostA:11222;hostB:11222).
         */
        // @formatter:on
        @ConfigItem
        public String hosts;

        // @formatter:off
        /**
         * Sets client intelligence used by authentication
         * Available values:
         * * `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list
         *              of servers supplied at configuration time.
         * * `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the
         *              remote TLS host.
         * * `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request
         *              involving keys will be routed to the server who is the primary owner which improves performance
         *              greatly. This is the default.
         */
        // @formatter:on
        @ConfigItem(defaultValue = "HASH_DISTRIBUTION_AWARE")
        Optional<String> clientIntelligence;

        // @formatter:off
        /**
         * Enables or disables Protobuf generated schemas upload to the backup.
         * Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself.
         * Default is 'true'.
         * This setting will be ignored if the Global Setting is set up to false.
         */
        // @formatter:on
        @ConfigItem(defaultValue = "true")
        Optional<Boolean> useSchemaRegistration;
    }

    @Override
    public String toString() {
        return "InfinispanClientRuntimeConfig{" +
                "hosts=" + hosts +
                '}';
    }
}
