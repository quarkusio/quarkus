package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchBuildTimeConfig;
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
     * Configuration for Elasticsearch/OpenSearch backends.
     */
    @ConfigDocSection
    @WithName("elasticsearch")
    @WithUnnamedKey // The default backend has the null key
    @ConfigDocMapKey("backend-name")
    Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> backends();

    /**
     * A xref:hibernate-search-standalone-elasticsearch.adoc#bean-reference-note-anchor[bean reference] to a component
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
     * See xref:hibernate-search-standalone-elasticsearch.adoc#plugging-in-custom-components[this section]
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
