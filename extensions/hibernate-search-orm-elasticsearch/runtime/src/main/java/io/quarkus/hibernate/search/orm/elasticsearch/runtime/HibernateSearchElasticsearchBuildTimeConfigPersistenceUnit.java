package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithName;
import io.smallrye.config.WithUnnamedKey;

@ConfigGroup
public interface HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit {

    /**
     * Configuration for Elasticsearch/OpenSearch backends.
     */
    @ConfigDocSection
    @WithName("elasticsearch")
    @WithUnnamedKey // The default backend has the null key
    @ConfigDocMapKey("backend-name")
    Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> backends();

    /**
     * A xref:hibernate-search-orm-elasticsearch.adoc#bean-reference-note-anchor[bean reference] to a component
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
     * See xref:hibernate-search-orm-elasticsearch.adoc#plugging-in-custom-components[this section]
     * for more information.
     *
     * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
     * ====
     *
     * @asciidoclet
     */
    Optional<String> backgroundFailureHandler();

    /**
     * Configuration for coordination between threads or application instances.
     */
    CoordinationConfig coordination();

    /**
     * Configuration for mapping.
     */
    MappingConfig mapping();

    @ConfigGroup
    interface CoordinationConfig {

        /**
         * The strategy to use for coordinating between threads or even separate instances of the application,
         * in particular in automatic indexing.
         *
         * See xref:hibernate-search-orm-elasticsearch.adoc#coordination[coordination] for more information.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("none")
        Optional<String> strategy();
    }

    @ConfigGroup
    interface MappingConfig {
        /**
         * One or more xref:hibernate-search-orm-elasticsearch.adoc#bean-reference-note-anchor[bean references]
         * to the component(s) used to configure the Hibernate Search mapping,
         * in particular programmatically.
         *
         * The referenced beans must implement `HibernateOrmSearchMappingConfigurer`.
         *
         * See xref:hibernate-search-orm-elasticsearch.adoc#programmatic-mapping[Programmatic mapping] for an example
         * on how mapping configurers can be used to apply programmatic mappings.
         *
         * [NOTE]
         * ====
         * Instead of setting this configuration property,
         * you can simply annotate your custom `HibernateOrmSearchMappingConfigurer` implementations with `@SearchExtension`
         * and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically.
         * See xref:hibernate-search-orm-elasticsearch.adoc#plugging-in-custom-components[this section]
         * for more information.
         *
         * If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.
         * ====
         *
         * @asciidoclet
         */
        Optional<List<String>> configurer();
    }

}
