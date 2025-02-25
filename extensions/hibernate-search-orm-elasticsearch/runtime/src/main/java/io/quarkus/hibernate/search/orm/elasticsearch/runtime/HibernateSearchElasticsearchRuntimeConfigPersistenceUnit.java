package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchRuntimeConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
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
     * Configuration for Elasticsearch/OpenSearch backends.
     */
    @ConfigDocSection
    @WithName("elasticsearch")
    @WithUnnamedKey // The default backend has the null key
    @ConfigDocMapKey("backend-name")
    Map<String, HibernateSearchBackendElasticsearchRuntimeConfig> backends();

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
         * See xref:hibernate-search-orm-elasticsearch.adoc#plugging-in-custom-components[this section]
         * for more information.
         *
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
         * See link:{hibernate-search-docs-url}#mapper-orm-schema-management-strategy[this section of the reference documentation]
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
