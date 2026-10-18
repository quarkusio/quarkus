package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchRuntimeConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-search-standalone")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HibernateSearchStandaloneRuntimeConfig {

    /**
     * Whether Hibernate Search Standalone should be active at runtime.
     *
     * If Hibernate Search Standalone is not active, it won't start with the application,
     * and accessing the SearchMapping for search or other operations will not be possible.
     *
     * Note that if Hibernate Search Standalone is disabled
     * (i.e. `quarkus.hibernate-search-standalone.enabled` is set to `false`),
     * it won't be active, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("'true' if Hibernate Search Standalone is enabled; 'false' otherwise")
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
     * Configuration for indexing.
     */
    IndexingConfig indexing();

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
         * after a `SearchSession` is closed.
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
         * See link:{hibernate-search-docs-url}#schema-management-strategy[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("create-or-validate")
        @ConfigDocDefault("drop-and-create-and-drop when using Dev Services; create-or-validate otherwise")
        SchemaManagementStrategyName strategy();

    }

    static String extensionPropertyKey(String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-standalone.");
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    static String mapperPropertyKey(String radical) {
        return "quarkus.hibernate-search-standalone." + radical;
    }

    static List<String> mapperPropertyKeys(String radical) {
        return List.of(mapperPropertyKey(radical));
    }

    static String backendPropertyKey(String backendName, String indexName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-standalone.");
        keyBuilder.append("elasticsearch.");
        if (backendName != null) {
            keyBuilder.append("\"").append(backendName).append("\".");
        }
        if (indexName != null) {
            keyBuilder.append("indexes.\"").append(indexName).append("\".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
    }

    static List<String> defaultBackendPropertyKeys(String radical) {
        return mapperPropertyKeys("elasticsearch." + radical);
    }
}
