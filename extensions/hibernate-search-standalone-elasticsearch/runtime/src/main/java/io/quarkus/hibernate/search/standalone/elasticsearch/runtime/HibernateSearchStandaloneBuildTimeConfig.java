package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.hibernate-search-standalone")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateSearchStandaloneBuildTimeConfig {

    /**
     * Whether Hibernate Search Standalone is enabled **during the build**.
     *
     * If Hibernate Search is disabled during the build, all processing related to Hibernate Search will be skipped,
     * but it will not be possible to activate Hibernate Search at runtime:
     * `quarkus.hibernate-search-standalone.active` will default to `false` and setting it to `true` will lead to an error.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Configuration for backends.
     */
    @ConfigDocSection
    @WithName("elasticsearch")
    @WithUnnamedKey // The default backend has the null key
    @ConfigDocMapKey("backend-name")
    Map<String, ElasticsearchBackendBuildTimeConfig> backends();

    /**
     * A xref:hibernate-search-stqndqlone-elasticsearch.adoc#bean-reference-note-anchor[bean reference] to a component
     * that should be notified of any failure occurring in a background process
     * (mainly index operations).
     *
     * The referenced bean must implement `FailureHandler`.
     *
     * See
     * link:{hibernate-search-docs-url}#configuration-background-failure-handling[this section of the reference documentation]
     * for more information.
     *
     * [NOTE]
     * ====
     * Instead of setting this configuration property,
     * you can simply annotate your custom `FailureHandler` implementation with `@SearchExtension`
     * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
     * See xref:hibernate-search-stqndqlone-elasticsearch.adoc#plugging-in-custom-components[this section]
     * for more information.
     *
     * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
     * ====
     *
     * @asciidoclet
     */
    Optional<String> backgroundFailureHandler();

    /**
     * Management interface.
     */
    @ConfigDocSection
    HibernateSearchStandaloneBuildTimeConfigManagement management();

    /**
     * Configuration related to the mapping.
     */
    MappingConfig mapping();

    @ConfigGroup
    interface ElasticsearchBackendBuildTimeConfig {
        /**
         * The version of Elasticsearch used in the cluster.
         *
         * As the schema is generated without a connection to the server, this item is mandatory.
         *
         * It doesn't have to be the exact version (it can be `7` or `7.1` for instance) but it has to be sufficiently precise
         * to choose a model dialect (the one used to generate the schema) compatible with the protocol dialect (the one used
         * to communicate with Elasticsearch).
         *
         * There's no rule of thumb here as it depends on the schema incompatibilities introduced by Elasticsearch versions. In
         * any case, if there is a problem, you will have an error when Hibernate Search tries to connect to the cluster.
         *
         * @asciidoclet
         */
        Optional<ElasticsearchVersion> version();

        /**
         * The default configuration for the Elasticsearch indexes.
         */
        @WithParentName
        ElasticsearchIndexBuildTimeConfig indexDefaults();

        /**
         * Per-index configuration overrides.
         */
        @ConfigDocSection
        @ConfigDocMapKey("index-name")
        Map<String, ElasticsearchIndexBuildTimeConfig> indexes();
    }

    @ConfigGroup
    interface ElasticsearchIndexBuildTimeConfig {
        /**
         * Configuration for automatic creation and validation of the Elasticsearch schema:
         * indexes, their mapping, their settings.
         */
        SchemaManagementConfig schemaManagement();

        /**
         * Configuration for full-text analysis.
         */
        AnalysisConfig analysis();
    }

    @ConfigGroup
    interface SchemaManagementConfig {

        // @formatter:off
        /**
         * Path to a file in the classpath holding custom index settings to be included in the index definition
         * when creating an Elasticsearch index.
         *
         * The provided settings will be merged with those generated by Hibernate Search, including analyzer definitions.
         * When analysis is configured both through an analysis configurer and these custom settings, the behavior is undefined;
         * it should not be relied upon.
         *
         * See link:{hibernate-search-docs-url}#backend-elasticsearch-configuration-index-settings[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        Optional<String> settingsFile();

        // @formatter:off
        /**
         * Path to a file in the classpath holding a custom index mapping to be included in the index definition
         * when creating an Elasticsearch index.
         *
         * The file does not need to (and generally shouldn't) contain the full mapping:
         * Hibernate Search will automatically inject missing properties (index fields) in the given mapping.
         *
         * See link:{hibernate-search-docs-url}#backend-elasticsearch-mapping-custom[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        Optional<String> mappingFile();

    }

    @ConfigGroup
    interface AnalysisConfig {
        /**
         * One or more xref:hibernate-search-standalone-elasticsearch.adoc#bean-reference-note-anchor[bean references]
         * to the component(s) used to configure full text analysis (e.g. analyzers, normalizers).
         *
         * The referenced beans must implement `ElasticsearchAnalysisConfigurer`.
         *
         * See xref:hibernate-search-standalone-elasticsearch.adoc#analysis-configurer[Setting up the analyzers] for more
         * information.
         *
         * [NOTE]
         * ====
         * Instead of setting this configuration property,
         * you can simply annotate your custom `ElasticsearchAnalysisConfigurer` implementations with `@SearchExtension`
         * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
         * See xref:hibernate-search-standalone-elasticsearch.adoc#plugging-in-custom-components[this section]
         * for more information.
         *
         * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
         * ====
         *
         * @asciidoclet
         */
        Optional<List<String>> configurer();
    }

    @ConfigGroup
    interface MappingConfig {
        /**
         * One or more xref:hibernate-search-standalone-elasticsearch.adoc#bean-reference-note-anchor[bean references]
         * to the component(s) used to configure the Hibernate Search mapping,
         * in particular programmatically.
         *
         * The referenced beans must implement `StandalonePojoMappingConfigurer`.
         *
         * See xref:hibernate-search-standalone-elasticsearch.adoc#programmatic-mapping[Programmatic mapping] for an example
         * on how mapping configurers can be used to apply programmatic mappings.
         *
         * [NOTE]
         * ====
         * Instead of setting this configuration property,
         * you can simply annotate your custom `StandalonePojoMappingConfigurer` implementations with `@SearchExtension`
         * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
         * See xref:hibernate-search-standalone-elasticsearch.adoc#plugging-in-custom-components[this section]
         * for more information.
         *
         * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
         * ====
         *
         * @asciidoclet
         */
        Optional<List<String>> configurer();

        // @formatter:off
        /**
         * The structure of the Hibernate Search entity mapping.
         *
         * This must match the structure of the application model being indexed with Hibernate Search:
         *
         * `graph` (default)::
         * Entities indexed through Hibernate Search are nodes in an entity graph,
         * i.e. an indexed entity is independent of other entities it references through associations,
         * which *can* be updated independently of the indexed entity.
         * +
         * Associations between entities must be bi-directional:
         * specifying the inverse side of associations through `@AssociationInverseSide` *is required*,
         * unless reindexing is disabled for that association through `@IndexingDependency(reindexOnUpdate = ...)`.
         * `document`::
         * Entities indexed through Hibernate Search are the root of a document,
         * i.e. an indexed entity "owns" other entities it references through associations,
         * which *cannot* be updated independently of the indexed entity.
         * +
         * Associations between entities can be uni-directional:
         * specifying the inverse side of associations through `@AssociationInverseSide` *is not required*.
         *
         * See also link:{hibernate-search-docs-url}#mapping-reindexing-associationinverseside[`@AssociationInverseSide`]
         * link:{hibernate-search-docs-url}#mapping-reindexing-reindexonupdate[`@IndexingDependency(reindexOnUpdate = ...)`].
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("graph")
        MappingStructure structure();
    }

}
