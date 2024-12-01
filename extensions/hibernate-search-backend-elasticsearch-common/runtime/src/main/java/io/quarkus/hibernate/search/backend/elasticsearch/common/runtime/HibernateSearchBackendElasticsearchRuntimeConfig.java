package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.SearchException;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface HibernateSearchBackendElasticsearchRuntimeConfig {
    /**
     * The list of hosts of the Elasticsearch servers.
     */
    @WithDefault("localhost:9200")
    List<String> hosts();

    /**
     * The protocol to use when contacting Elasticsearch servers.
     * Set to "https" to enable SSL/TLS.
     */
    @WithDefault("http")
    ElasticsearchClientProtocol protocol();

    /**
     * The username used for authentication.
     */
    Optional<String> username();

    /**
     * The password used for authentication.
     */
    Optional<String> password();

    /**
     * The timeout when establishing a connection to an Elasticsearch server.
     */
    @WithDefault("1S")
    Duration connectionTimeout();

    /**
     * The timeout when reading responses from an Elasticsearch server.
     */
    @WithDefault("30S")
    Duration readTimeout();

    /**
     * The timeout when executing a request to an Elasticsearch server.
     *
     * This includes the time needed to wait for a connection to be available,
     * send the request and read the response.
     *
     * @asciidoclet
     */
    Optional<Duration> requestTimeout();

    /**
     * The maximum number of connections to all the Elasticsearch servers.
     */
    @WithDefault("20")
    int maxConnections();

    /**
     * The maximum number of connections per Elasticsearch server.
     */
    @WithDefault("10")
    int maxConnectionsPerRoute();

    /**
     * Configuration for the automatic discovery of new Elasticsearch nodes.
     */
    DiscoveryConfig discovery();

    /**
     * Configuration for the thread pool assigned to the backend.
     */
    ThreadPoolConfig threadPool();

    /**
     * Configuration for search queries to this backend.
     */
    QueryConfig query();

    /**
     * Configuration for version checks on this backend.
     */
    VersionCheckConfig versionCheck();

    /**
     * The default configuration for the Elasticsearch indexes.
     */
    @WithParentName
    IndexConfig indexDefaults();

    /**
     * Per-index configuration overrides.
     */
    @ConfigDocSection
    @ConfigDocMapKey("index-name")
    Map<String, IndexConfig> indexes();

    /**
     * Configuration for the index layout.
     */
    LayoutConfig layout();

    enum ElasticsearchClientProtocol {
        /**
         * Use clear-text HTTP, with SSL/TLS disabled.
         */
        HTTP("http"),
        /**
         * Use HTTPS, with SSL/TLS enabled.
         */
        HTTPS("https");

        public static ElasticsearchClientProtocol of(String value) {
            return ParseUtils.parseDiscreteValues(
                    values(),
                    ElasticsearchClientProtocol::getHibernateSearchString,
                    (invalidValue, validValues) -> new SearchException(
                            String.format(
                                    Locale.ROOT,
                                    "Invalid protocol: '%1$s'. Valid protocols are: %2$s.",
                                    invalidValue,
                                    validValues)),
                    value);
        }

        private final String hibernateSearchString;

        ElasticsearchClientProtocol(String hibernateSearchString) {
            this.hibernateSearchString = hibernateSearchString;
        }

        public String getHibernateSearchString() {
            return hibernateSearchString;
        }
    }

    @ConfigGroup
    interface VersionCheckConfig {
        /**
         * Whether Hibernate Search should check the version of the Elasticsearch cluster on startup.
         *
         * Set to `false` if the Elasticsearch cluster may not be available on startup.
         *
         * @asciidoclet
         */
        @WithDefault("true")
        boolean enabled();
    }

    @ConfigGroup
    interface IndexConfig {
        /**
         * Configuration for the schema management of the indexes.
         */
        SchemaManagementConfig schemaManagement();

        /**
         * Configuration for the indexing process that creates, updates and deletes documents.
         */
        IndexingConfig indexing();
    }

    @ConfigGroup
    interface DiscoveryConfig {

        /**
         * Defines if automatic discovery is enabled.
         */
        @WithDefault("false")
        Boolean enabled();

        /**
         * Refresh interval of the node list.
         */
        @WithDefault("10S")
        Duration refreshInterval();

    }

    @ConfigGroup
    interface ThreadPoolConfig {
        /**
         * The size of the thread pool assigned to the backend.
         *
         * Note that number is **per backend**, not per index.
         * Adding more indexes will not add more threads.
         *
         * As all operations happening in this thread-pool are non-blocking,
         * raising its size above the number of processor cores available to the JVM will not bring noticeable performance
         * benefit.
         * The only reason to alter this setting would be to reduce the number of threads;
         * for example, in an application with a single index with a single indexing queue,
         * running on a machine with 64 processor cores,
         * you might want to bring down the number of threads.
         *
         * Defaults to the number of processor cores available to the JVM on startup.
         *
         * @asciidoclet
         */
        // We can't set an actual default value here: see comment on this class.
        OptionalInt size();
    }

    @ConfigGroup
    interface QueryConfig {
        /**
         * Configuration for the behavior on shard failure.
         */
        QueryShardFailureConfig shardFailure();
    }

    @ConfigGroup
    interface QueryShardFailureConfig {
        /**
         * Whether partial shard failures are ignored (`true`)
         * or lead to Hibernate Search throwing an exception (`false`).
         */
        @WithDefault("false")
        boolean ignore();
    }

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search-orm.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    interface SchemaManagementConfig {
        /**
         * The minimal https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html[Elasticsearch cluster
         * status] required on startup.
         *
         * @asciidoclet
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigDocDefault("yellow")
        Optional<IndexStatus> requiredStatus();

        /**
         * How long we should wait for the status before failing the bootstrap.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigDocDefault("10S")
        Optional<Duration> requiredStatusWaitTimeout();
    }

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search-orm.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    interface IndexingConfig {
        /**
         * The number of indexing queues assigned to each index.
         *
         * Higher values will lead to more connections being used in parallel,
         * which may lead to higher indexing throughput,
         * but incurs a risk of overloading Elasticsearch,
         * i.e. of overflowing its HTTP request buffers and tripping
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html[circuit breakers],
         * leading to Elasticsearch giving up on some request and resulting in indexing failures.
         *
         * @asciidoclet
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigDocDefault("10")
        OptionalInt queueCount();

        /**
         * The size of indexing queues.
         *
         * Lower values may lead to lower memory usage, especially if there are many queues,
         * but values that are too low will reduce the likeliness of reaching the max bulk size
         * and increase the likeliness of application threads blocking because the queue is full,
         * which may lead to lower indexing throughput.
         *
         * @asciidoclet
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigDocDefault("1000")
        OptionalInt queueSize();

        /**
         * The maximum size of bulk requests created when processing indexing queues.
         *
         * Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch,
         * which may lead to higher indexing throughput,
         * but incurs a risk of overloading Elasticsearch,
         * i.e. of overflowing its HTTP request buffers and tripping
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html[circuit breakers],
         * leading to Elasticsearch giving up on some request and resulting in indexing failures.
         *
         * Note that raising this number above the queue size has no effect,
         * as bulks cannot include more requests than are contained in the queue.
         *
         * @asciidoclet
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigDocDefault("100")
        OptionalInt maxBulkSize();
    }

    @ConfigGroup
    interface LayoutConfig {
        /**
         * A xref:#bean-reference-note-anchor[bean reference] to the component
         * used to configure the Elasticsearch layout: index names, index aliases, ...
         *
         * The referenced bean must implement `IndexLayoutStrategy`.
         *
         * Available built-in implementations:
         *
         * `simple`::
         * The default, future-proof strategy: if the index name in Hibernate Search is `myIndex`,
         * this strategy will create an index named `myindex-000001`, an alias for write operations named `myindex-write`,
         * and an alias for read operations named `myindex-read`.
         * `no-alias`::
         * A strategy without index aliases, mostly useful on legacy clusters:
         * if the index name in Hibernate Search is `myIndex`,
         * this strategy will create an index named `myindex`, and will not use any alias.
         *
         * See
         * link:{hibernate-search-docs-url}#backend-elasticsearch-indexlayout[this section of the reference documentation]
         * for more information.
         *
         * [NOTE]
         * ====
         * Instead of setting this configuration property,
         * you can simply annotate your custom `IndexLayoutStrategy` implementation with `@SearchExtension`
         * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
         * See xref:#plugging-in-custom-components[this section]
         * for more information.
         *
         * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
         * ====
         *
         * @asciidoclet
         */
        Optional<String> strategy();
    }
}
