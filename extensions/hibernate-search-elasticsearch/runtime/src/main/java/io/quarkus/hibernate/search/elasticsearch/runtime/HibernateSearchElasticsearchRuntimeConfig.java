package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.StringHelper;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search", phase = ConfigPhase.RUN_TIME)
public class HibernateSearchElasticsearchRuntimeConfig {

    /**
     * Default backend
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    ElasticsearchBackendRuntimeConfig defaultBackend;

    /**
     * Additional backends
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchAdditionalBackendsRuntimeConfig additionalBackends;

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
    public static class ElasticsearchAdditionalBackendsRuntimeConfig {

        /**
         * Additional backends
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
         * The connection timeout.
         */
        @ConfigItem(defaultValue = "3S")
        Duration connectionTimeout;

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
         * The default configuration for the Elasticsearch indexes.
         */
        @ConfigItem
        ElasticsearchIndexConfig indexDefaults;

        /**
         * Per-index specific configuration.
         */
        @ConfigItem
        @ConfigDocMapKey("index-name")
        Map<String, ElasticsearchIndexConfig> indexes;
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
            return StringHelper.parseDiscreteValues(
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
    public static class ElasticsearchIndexConfig {
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
        @ConfigItem(defaultValue = "false")
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

        /**
         * The synchronization strategy to use when indexing automatically.
         * <p>
         * Defines how complete indexing should be before resuming the application thread
         * after a database transaction is committed.
         * <p>
         * Available values:
         * <table>
         * <thead>
         * <tr>
         * <th rowspan="2">
         * <p>
         * Strategy
         * </p>
         * </th>
         * <th colspan="3">
         * <p>
         * Guarantees when the application thread resumes
         * </p>
         * </th>
         * <th rowspan="2">
         * <p>
         * Throughput
         * </p>
         * </th>
         * </tr>
         * <tr>
         * <th>
         * <p>
         * Changes applied
         * </p>
         * </th>
         * <th>
         * <p>
         * Changes safe from crash/power loss
         * </p>
         * </th>
         * <th>
         * <p>
         * Changes visible on search
         * </p>
         * </th>
         * </tr>
         * </thead>
         * <tbody>
         * <tr>
         * <td>
         * <p>
         * <code>{@value AutomaticIndexingSynchronizationStrategyNames#ASYNC}</code>
         * </p>
         * </td>
         * <td>
         * <p>
         * No guarantee
         * </p>
         * </td>
         * <td>
         * <p>
         * No guarantee
         * </p>
         * </td>
         * <td>
         * <p>
         * No guarantee
         * </p>
         * </td>
         * <td>
         * <p>
         * Best
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>
         * <p>
         * <code>{@value AutomaticIndexingSynchronizationStrategyNames#WRITE_SYNC}</code> (<strong>default</strong>)
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * No guarantee
         * </p>
         * </td>
         * <td>
         * <p>
         * Medium
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>
         * <p>
         * <code>{@value AutomaticIndexingSynchronizationStrategyNames#READ_SYNC}</code>
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * No guarantee
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * Medium to worst
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>
         * <p>
         * <code>{@value AutomaticIndexingSynchronizationStrategyNames#SYNC}</code>
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * Guaranteed
         * </p>
         * </td>
         * <td>
         * <p>
         * Worst
         * </p>
         * </td>
         * </tr>
         * </tbody>
         * </table>
         * <p>
         * See
         * <a href=
         * "https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-indexing-automatic-synchronization">this
         * section of the reference documentation</a>
         * for more information.
         */
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

        /**
         * The strategy used for index lifecycle.
         */
        // We can't set an actual default value here: see comment on this class.
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
    // otherwise "quarkus.hibernate-search.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    public static class ElasticsearchIndexSchemaManagementConfig {
        /**
         * The minimal cluster status required.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "green")
        Optional<IndexStatus> requiredStatus;

        /**
         * How long we should wait for the status before failing the bootstrap.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "10S")
        Optional<Duration> requiredStatusWaitTimeout;
    }

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    public static class ElasticsearchIndexIndexingConfig {
        /**
         * The number of indexing queues assigned to each index.
         * <p>
         * Higher values will lead to more connections being used in parallel,
         * which may lead to higher indexing throughput,
         * but incurs a risk of overloading Elasticsearch,
         * i.e. of overflowing its HTTP request buffers and tripping
         * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.6/circuit-breaker.html">circuit breakers</a>,
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
         * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.6/circuit-breaker.html">circuit breakers</a>,
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
