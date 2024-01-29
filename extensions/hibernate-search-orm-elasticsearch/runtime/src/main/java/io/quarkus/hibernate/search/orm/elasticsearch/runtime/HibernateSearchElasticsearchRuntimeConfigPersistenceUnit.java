package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.SearchException;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigGroup
public interface HibernateSearchElasticsearchRuntimeConfigPersistenceUnit {

    /**
     * Whether Hibernate Search should be active for this persistence unit at runtime.
     *
     * If Hibernate Search is not active, it won't index Hibernate ORM entities,
     * and accessing the SearchMapping/SearchSession of the relevant persistence unit
     * for search or other operation will not be possible.
     *
     * Note that if Hibernate Search is disabled (i.e. `quarkus.hibernate-search-orm.enabled` is set to `false`),
     * it won't be active for any persistence unit, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("'true' if Hibernate Search is enabled; 'false' otherwise")
    Optional<Boolean> active();

    /**
     * Configuration for backends.
     */
    @ConfigDocSection
    @WithName("elasticsearch")
    @WithUnnamedKey // The default backend has the null key
    @ConfigDocMapKey("backend-name")
    Map<String, ElasticsearchBackendRuntimeConfig> backends();

    /**
     * Configuration for automatic creation and validation of the Elasticsearch schema:
     * indexes, their mapping, their settings.
     */
    SchemaManagementConfig schemaManagement();

    /**
     * Configuration for how entities are loaded by a search query.
     */
    @WithName("query.loading")
    SearchQueryLoadingConfig queryLoading();

    /**
     * Configuration for indexing.
     */
    IndexingConfig indexing();

    /**
     * Configuration for automatic indexing.
     *
     * @deprecated Use {@link #indexing()} instead.
     */
    @Deprecated
    AutomaticIndexingConfig automaticIndexing();

    /**
     * Configuration for multi-tenancy.
     */
    MultiTenancyConfig multiTenancy();

    @ConfigGroup
    interface ElasticsearchBackendRuntimeConfig {
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
        ElasticsearchQueryConfig query();

        /**
         * Configuration for version checks on this backend.
         */
        ElasticsearchVersionCheckConfig versionCheck();

        /**
         * The default configuration for the Elasticsearch indexes.
         */
        @WithParentName
        ElasticsearchIndexRuntimeConfig indexDefaults();

        /**
         * Per-index configuration overrides.
         */
        @ConfigDocSection
        @ConfigDocMapKey("index-name")
        Map<String, ElasticsearchIndexRuntimeConfig> indexes();
    }

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
    interface ElasticsearchVersionCheckConfig {
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
    interface ElasticsearchIndexRuntimeConfig {
        /**
         * Configuration for the schema management of the indexes.
         */
        ElasticsearchIndexSchemaManagementConfig schemaManagement();

        /**
         * Configuration for the indexing process that creates, updates and deletes documents.
         */
        ElasticsearchIndexIndexingConfig indexing();
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
    interface IndexingConfig {

        /**
         * Configuration for indexing plans.
         */
        IndexingPlanConfig plan();

    }

    // Having a dedicated config group class feels a bit unnecessary
    // because we could just use @WithName("plan.synchronization.strategy")
    // but that leads to bugs
    // see https://github.com/quarkusio/quarkus/pull/34251#issuecomment-1611273375
    @ConfigGroup
    interface IndexingPlanConfig {

        /**
         * Configuration for indexing plan synchronization.
         */
        IndexingPlanSynchronizationConfig synchronization();

    }

    // Having a dedicated config group class feels a bit unnecessary
    // because we could just use @WithName("plan.synchronization.strategy")
    // but that leads to bugs
    // see https://github.com/quarkusio/quarkus/pull/34251#issuecomment-1611273375
    @ConfigGroup
    interface IndexingPlanSynchronizationConfig {

        // @formatter:off
        /**
         * How to synchronize between application threads and indexing,
         * in particular when relying on (implicit) listener-triggered indexing on entity change,
         * but also when using a `SearchIndexingPlan` explicitly.
         *
         * Defines how complete indexing should be before resuming the application thread
         * after a database transaction is committed.
         *
         * [WARNING]
         * ====
         * Indexing synchronization is only relevant when coordination is disabled (which is the default).
         *
         * With the xref:hibernate-search-orm-elasticsearch.adoc#coordination[`outbox-polling` coordination strategy],
         * indexing happens in background threads and is always asynchronous;
         * the behavior is equivalent to the `write-sync` synchronization strategy.
         * ====
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
         * This property also accepts a xref:hibernate-search-orm-elasticsearch.adoc#bean-reference-note-anchor[bean reference]
         * to a custom implementations of `IndexingPlanSynchronizationStrategy`.
         *
         * See
         * link:{hibernate-search-docs-url}#indexing-plan-synchronization[this section of the reference documentation]
         * for more information.
         *
         * [NOTE]
         * ====
         * Instead of setting this configuration property,
         * you can simply annotate your custom `IndexingPlanSynchronizationStrategy` implementation with `@SearchExtension`
         * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
         * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
         * ====
         *
         * @asciidoclet
         */
        // @formatter:on
        @ConfigDocDefault("write-sync")
        Optional<String> strategy();

    }

    @ConfigGroup
    @Deprecated
    interface AutomaticIndexingConfig {

        /**
         * Configuration for synchronization with the index when indexing automatically.
         *
         * @deprecated Use {@code quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy} instead.
         */
        AutomaticIndexingSynchronizationConfig synchronization();

        /**
         * Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.
         * <p>
         * When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when
         * indexing.
         *
         * @deprecated This property is deprecated with no alternative to replace it.
         *             In the future, a dirty check will always be performed when considering whether to trigger reindexing.
         */
        @WithDefault("true")
        @Deprecated
        boolean enableDirtyCheck();
    }

    @ConfigGroup
    @Deprecated
    interface AutomaticIndexingSynchronizationConfig {

        // @formatter:off
        /**
         * The synchronization strategy to use when indexing automatically.
         *
         * @deprecated Use {@code quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy} instead.
         */
        // @formatter:on
        @ConfigDocDefault("write-sync")
        Optional<String> strategy();
    }

    @ConfigGroup
    interface SearchQueryLoadingConfig {

        /**
         * Configuration for cache lookup when loading entities during the execution of a search query.
         */
        SearchQueryLoadingCacheLookupConfig cacheLookup();

        /**
         * The fetch size to use when loading entities during the execution of a search query.
         */
        @WithDefault("100")
        int fetchSize();
    }

    @ConfigGroup
    interface SearchQueryLoadingCacheLookupConfig {

        /**
         * The strategy to use when loading entities during the execution of a search query.
         */
        @WithDefault("skip")
        EntityLoadingCacheLookupStrategy strategy();
    }

    @ConfigGroup
    interface SchemaManagementConfig {

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
         * !create-or-validate (**default** unless using Dev Services)
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
         * !drop-and-create-and-drop (**default** when using Dev Services)
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
        @WithDefault("create-or-validate")
        @ConfigDocDefault("drop-and-create-and-drop when using Dev Services; create-or-validate otherwise")
        SchemaManagementStrategyName strategy();

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
    interface ElasticsearchQueryConfig {
        /**
         * Configuration for the behavior on shard failure.
         */
        ElasticsearchQueryShardFailureConfig shardFailure();
    }

    @ConfigGroup
    interface ElasticsearchQueryShardFailureConfig {
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
    interface ElasticsearchIndexSchemaManagementConfig {
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
    interface ElasticsearchIndexIndexingConfig {
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
    interface MultiTenancyConfig {

        /**
         * An exhaustive list of all tenant identifiers that may be used by the application when multi-tenancy is enabled.
         *
         * Mainly useful when using the {@code outbox-polling} coordination strategy,
         * since it involves setting up one background processor per tenant.
         *
         * @asciidoclet
         */
        Optional<List<String>> tenantIds();

    }
}
