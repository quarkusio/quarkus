package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit {

    /**
     * Default backend
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchBackendBuildTimeConfig defaultBackend;

    /**
     * Named backends
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchNamedBackendsBuildTimeConfig namedBackends;

    /**
     * A <<bean-reference-note-anchor,bean reference>> to a component
     * that should be notified of any failure occurring in a background process
     * (mainly index operations).
     *
     * The referenced bean must implement `FailureHandler`.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> backgroundFailureHandler;

    @ConfigGroup
    public static class ElasticsearchNamedBackendsBuildTimeConfig {

        /**
         * Named backends
         */
        @ConfigDocMapKey("backend-name")
        public Map<String, ElasticsearchBackendBuildTimeConfig> backends;

    }

    @ConfigGroup
    public static class ElasticsearchBackendBuildTimeConfig {
        /**
         * The version of Elasticsearch used in the cluster.
         * <p>
         * As the schema is generated without a connection to the server, this item is mandatory.
         * <p>
         * It doesn't have to be the exact version (it can be 7 or 7.1 for instance) but it has to be sufficiently precise to
         * choose a model dialect (the one used to generate the schema) compatible with the protocol dialect (the one used to
         * communicate with Elasticsearch).
         * <p>
         * There's no rule of thumb here as it depends on the schema incompatibilities introduced by Elasticsearch versions. In
         * any case, if there is a problem, you will have an error when Hibernate Search tries to connect to the cluster.
         */
        @ConfigItem
        public Optional<ElasticsearchVersion> version;

        /**
         * Configuration for the index layout.
         */
        @ConfigItem
        public LayoutConfig layout;

        /**
         * The default configuration for the Elasticsearch indexes.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public ElasticsearchIndexBuildTimeConfig indexDefaults;

        /**
         * Per-index specific configuration.
         */
        @ConfigItem
        @ConfigDocMapKey("index-name")
        public Map<String, ElasticsearchIndexBuildTimeConfig> indexes;
    }

    @ConfigGroup
    public static class ElasticsearchIndexBuildTimeConfig {
        /**
         * Configuration for full-text analysis.
         */
        @ConfigItem
        public AnalysisConfig analysis;
    }

    @ConfigGroup
    public static class AnalysisConfig {
        /**
         * A <<bean-reference-note-anchor,bean reference>> to the component
         * used to configure full text analysis (e.g. analyzers, normalizers).
         *
         * The referenced bean must implement `ElasticsearchAnalysisConfigurer`.
         *
         * See <<analysis-configurer>> for more information.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<String> configurer;
    }

    @ConfigGroup
    public static class LayoutConfig {
        /**
         * A <<bean-reference-note-anchor,bean reference>> to the component
         * used to configure layout (e.g. index names, index aliases).
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
         * link:{hibernate-search-doc-prefix}#backend-elasticsearch-indexlayout[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        @ConfigItem
        public Optional<String> strategy;
    }
}
