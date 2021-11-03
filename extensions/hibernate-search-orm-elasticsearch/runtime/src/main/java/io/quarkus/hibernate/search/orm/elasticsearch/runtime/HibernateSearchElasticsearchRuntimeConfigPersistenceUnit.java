package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.SearchException;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HibernateSearchElasticsearchRuntimeConfigPersistenceUnit {

    /**
     * Whether Hibernate Search is enabled.
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

    /**
     * Default backend
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    ElasticsearchBackendRuntimeConfig defaultBackend;

    /**
     * Named backends
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchNamedBackendsRuntimeConfig namedBackends;

    /**
     * Configuration for automatic creation and validation of the Elasticsearch schema:
     * indexes, their mapping, their settings.
     */
    @ConfigItem
    SchemaManagementConfig schemaManagement;

    /**
     * Configuration for how entities are loaded by a search query.
     */
    @ConfigItem(name = "query.loading")
    SearchQueryLoadingConfig queryLoading;

    /**
     * Configuration for the automatic indexing.
     */
    @ConfigItem
    AutomaticIndexingConfig automaticIndexing;

    @ConfigGroup
    public static class ElasticsearchNamedBackendsRuntimeConfig {

        /**
         * Named backends
         */
        @ConfigDocMapKey("backend-name")
        public Map<String, ElasticsearchBackendRuntimeConfig> backends;

    }

    @ConfigGroup
    public static class ElasticsearchBackendRuntimeConfig {
        /**
         * The list of hosts of the Elasticsearch servers.
         */
        @ConfigItem(defaultValue = "localhost:9200")
        List<String> hosts;

        /**
         * The protocol to use when contacting Elasticsearch servers.
         * Set to "https" to enable SSL/TLS.
         */
        @ConfigItem(defaultValue = "http")
        ElasticsearchClientProtocol protocol;

        /**
         * The username used for authentication.
         */
        @ConfigItem
        Optional<String> username;

        /**
         * The password used for authentication.
         */
        @ConfigItem
        Optional<String> password;

        /**
         * The timeout when establishing a connection to an Elasticsearch server.
         */
        @ConfigItem(defaultValue = "1S")
        Duration connectionTimeout;

        /**
         * The timeout when reading responses from an Elasticsearch server.
         */
        @ConfigItem(defaultValue = "30S")
        Duration readTimeout;

        /**
         * The timeout when executing a request to an Elasticsearch server.
         * <p>
         * This includes the time needed to wait for a connection to be available,
         * send the request and read the response.
         */
        @ConfigItem
        Optional<Duration> requestTimeout;

        /**
         * The maximum number of connections to all the Elasticsearch servers.
         */
        @ConfigItem(defaultValue = "20")
        int maxConnections;

        /**
         * The maximum number of connections per Elasticsearch server.
         */
        @ConfigItem(defaultValue = "10")
        int maxConnectionsPerRoute;

        /**
         * Configuration for the automatic discovery of new Elasticsearch nodes.
         */
        @ConfigItem
        DiscoveryConfig discovery;

        /**
         * Configuration for the thread pool assigned to the backend.
         */
        @ConfigItem
        ThreadPoolConfig threadPool;

        /**
         * Whether Hibernate Search should check the version of the Elasticsearch cluster on startup.
         * <p>
         * Set to {@code false} if the Elasticsearch cluster may not be available on startup.
         */
        @ConfigItem(name = "version-check.enabled", defaultValue = "true")
        public boolean versionCheck;

        /**
         * The default configuration for the Elasticsearch indexes.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        ElasticsearchIndexRuntimeConfig indexDefaults;

        /**
         * Per-index specific configuration.
         */
        @ConfigItem
        @ConfigDocMapKey("index-name")
        Map<String, ElasticsearchIndexRuntimeConfig> indexes;
    }

    public enum ElasticsearchClientProtocol {
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
    public static class ElasticsearchIndexRuntimeConfig {
        /**
         * Configuration for the schema management of the indexes.
         */
        @ConfigItem
        ElasticsearchIndexSchemaManagementConfig schemaManagement;

        /**
         * Configuration for the indexing process that creates, updates and deletes documents.
         */
        @ConfigItem
        ElasticsearchIndexIndexingConfig indexing;
    }

    @ConfigGroup
    public static class DiscoveryConfig {

        /**
         * Defines if automatic discovery is enabled.
         */
        @ConfigItem
        boolean enabled;

        /**
         * Refresh interval of the node list.
         */
        @ConfigItem(defaultValue = "10S")
        Duration refreshInterval;

    }

    @ConfigGroup
    public static class AutomaticIndexingConfig {

        /**
         * Configuration for synchronization with the index when indexing automatically.
         */
        @ConfigItem
        AutomaticIndexingSynchronizationConfig synchronization;

        /**
         * Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.
         * <p>
         * When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when
         * indexing.
         */
        @ConfigItem(defaultValue = "true")
        boolean enableDirtyCheck;
    }

    @ConfigGroup
    public static class AutomaticIndexingSynchronizationConfig {

        // @formatter:off
        /**
         * The synchronization strategy to use when indexing automatically.
         *
         * Defines how complete indexing should be before resuming the application thread
         * after a database transaction is committed.
         *
         * Available values:
         *
         * [cols=5]
         * !===
         * .2+h!Strategy
         * .2+h!Throughput
         * 3+^h!Guarantees when the application thread resumes
         *
         * h!Changes applied
         * h!Changes safe from crash/power loss
         * h!Changes visible on search
         *
         * !async
         * !Best
         * ^!icon:times[role=red]
         * ^!icon:times[role=red]
         * ^!icon:times[role=red]
         *
         * !write-sync (**default**)
         * !Medium
         * ^!icon:check[role=lime]
         * ^!icon:check[role=lime]
         * ^!icon:times[role=red]
         *
         * !read-sync
         * !Medium to worst
         * ^!icon:check[role=lime]
         * ^!icon:times[role=red]
         * ^!icon:check[role=lime]
         *
         * !sync
         * !Worst
         * ^!icon:check[role=lime]
         * ^!icon:check[role=lime]
         * ^!icon:check[role=lime]
         * !===
         *
         * This property also accepts a <<bean-reference-note-anchor,bean reference>>
         * to a custom implementations of `AutomaticIndexingSynchronizationStrategy`.
         *
         * See
         * link:{hibernate-search-doc-prefix}#mapper-orm-indexing-automatic-synchronization[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @ConfigItem(defaultValue = AutomaticIndexingSynchronizationStrategyNames.WRITE_SYNC)
        String strategy;
    }

    @ConfigGroup
    public static class SearchQueryLoadingConfig {

        /**
         * Configuration for cache lookup when loading entities during the execution of a search query.
         */
        @ConfigItem
        SearchQueryLoadingCacheLookupConfig cacheLookup;

        /**
         * The fetch size to use when loading entities during the execution of a search query.
         */
        @ConfigItem(defaultValue = "100")
        int fetchSize;
    }

    @ConfigGroup
    public static class SearchQueryLoadingCacheLookupConfig {

        /**
         * The strategy to use when loading entities during the execution of a search query.
         */
        @ConfigItem(defaultValue = "skip")
        EntityLoadingCacheLookupStrategy strategy;
    }

    @ConfigGroup
    public static class SchemaManagementConfig {

        // @formatter:off
        /**
         * The schema management strategy, controlling how indexes and their schema
         * are created, updated, validated or dropped on startup and shutdown.
         *
         * Available values:
         *
         * [cols=2]
         * !===
         * h!Strategy
         * h!Definition
         *
         * !none
         * !Do nothing: assume that indexes already exist and that their schema matches Hibernate Search's expectations.
         *
         * !validate
         * !Validate that indexes exist and that their schema matches Hibernate Search's expectations.
         *
         * If it does not, throw an exception, but make no attempt to fix the problem.
         *
         * !create
         * !For indexes that do not exist, create them along with their schema.
         *
         * For indexes that already exist, do nothing: assume that their schema matches Hibernate Search's expectations.
         *
         * !create-or-validate (**default**)
         * !For indexes that do not exist, create them along with their schema.
         *
         * For indexes that already exist, validate that their schema matches Hibernate Search's expectations.
         *
         * If it does not, throw an exception, but make no attempt to fix the problem.
         *
         * !create-or-update
         * !For indexes that do not exist, create them along with their schema.
         *
         * For indexes that already exist, validate that their schema matches Hibernate Search's expectations;
         * if it does not match expectations, try to update it.
         *
         * **This strategy is unfit for production environments**,
         * due to several important limitations,
         * but can be useful when developing.
         *
         * !drop-and-create
         * !For indexes that do not exist, create them along with their schema.
         *
         * For indexes that already exist, drop them, then create them along with their schema.
         *
         * !drop-and-create-and-drop
         * !For indexes that do not exist, create them along with their schema.
         *
         * For indexes that already exist, drop them, then create them along with their schema.
         *
         * Also, drop indexes and their schema on shutdown.
         * !===
         *
         * See https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-orm-schema-management-strategy[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @ConfigItem(defaultValue = "create-or-validate")
        SchemaManagementStrategyName strategy;

    }

    @ConfigGroup
    public static class ThreadPoolConfig {
        /**
         * The size of the thread pool assigned to the backend.
         * <p>
         * Note that number is <em>per backend</em>, not per index.
         * Adding more indexes will not add more threads.
         * <p>
         * As all operations happening in this thread-pool are non-blocking,
         * raising its size above the number of processor cores available to the JVM will not bring noticeable performance
         * benefit.
         * The only reason to alter this setting would be to reduce the number of threads;
         * for example, in an application with a single index with a single indexing queue,
         * running on a machine with 64 processor cores,
         * you might want to bring down the number of threads.
         * <p>
         * Defaults to the number of processor cores available to the JVM on startup.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem
        OptionalInt size;
    }

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search-orm.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    public static class ElasticsearchIndexSchemaManagementConfig {
        /**
         * The minimal cluster status required.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "yellow")
        Optional<IndexStatus> requiredStatus;

        /**
         * How long we should wait for the status before failing the bootstrap.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "10S")
        Optional<Duration> requiredStatusWaitTimeout;
    }

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search-orm.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    public static class ElasticsearchIndexIndexingConfig {
        /**
         * The number of indexing queues assigned to each index.
         * <p>
         * Higher values will lead to more connections being used in parallel,
         * which may lead to higher indexing throughput,
         * but incurs a risk of overloading Elasticsearch,
         * i.e. of overflowing its HTTP request buffers and tripping
         * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html">circuit breakers</a>,
         * leading to Elasticsearch giving up on some request and resulting in indexing failures.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "10")
        OptionalInt queueCount;

        /**
         * The size of indexing queues.
         * <p>
         * Lower values may lead to lower memory usage, especially if there are many queues,
         * but values that are too low will reduce the likeliness of reaching the max bulk size
         * and increase the likeliness of application threads blocking because the queue is full,
         * which may lead to lower indexing throughput.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "1000")
        OptionalInt queueSize;

        /**
         * The maximum size of bulk requests created when processing indexing queues.
         * <p>
         * Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch,
         * which may lead to higher indexing throughput,
         * but incurs a risk of overloading Elasticsearch,
         * i.e. of overflowing its HTTP request buffers and tripping
         * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html">circuit breakers</a>,
         * leading to Elasticsearch giving up on some request and resulting in indexing failures.
         * <p>
         * Note that raising this number above the queue size has no effect,
         * as bulks cannot include more requests than are contained in the queue.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "100")
        OptionalInt maxBulkSize;
    }
}
