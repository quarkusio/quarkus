package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
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
         * Configuration for the lifecyle of the indexes.
         */
        @ConfigItem
        LifecycleConfig lifecycle;
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

    // We can't set actual default values in this section,
    // otherwise "quarkus.hibernate-search.elasticsearch.index-defaults" will be ignored.
    @ConfigGroup
    public static class LifecycleConfig {

        /**
         * The strategy used for index lifecycle.
         */
        // We can't set an actual default value here: see comment on this class.
        @ConfigItem(defaultValueDocumentation = "create")
        Optional<IndexLifecycleStrategyName> strategy;

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
}
