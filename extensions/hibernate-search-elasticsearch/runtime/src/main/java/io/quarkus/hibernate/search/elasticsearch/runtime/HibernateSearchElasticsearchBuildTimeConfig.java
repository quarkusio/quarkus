package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateSearchElasticsearchBuildTimeConfig {

    /**
     * Default backend
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchBackendBuildTimeConfig defaultBackend;

    /**
     * Additional backends
     */
    @ConfigItem(name = "elasticsearch")
    @ConfigDocSection
    public ElasticsearchAdditionalBackendsBuildTimeConfig additionalBackends;

    /**
     * The class or the name of the bean that should be notified of any failure occurring in a background process
     * (mainly index operations).
     * <p>
     * Must implement {@link org.hibernate.search.engine.reporting.FailureHandler}.
     */
    @ConfigItem
    public Optional<Class<?>> backgroundFailureHandler;

    @ConfigGroup
    public static class ElasticsearchAdditionalBackendsBuildTimeConfig {

        /**
         * Only useful when defining {@link #backends additional backends}:
         * the name of the default backend,
         * i.e. the backend that will be assigned to {@code @Indexed} entities
         * that do not specify a backend explicitly through {@code @Indexed(backend = ...)}.
         */
        @ConfigItem
        public Optional<String> defaultBackend;

        /**
         * Additional backends
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
         * Configuration for full-text analysis.
         */
        @ConfigItem
        public AnalysisConfig analysis;
    }

    @ConfigGroup
    public static class AnalysisConfig {
        /**
         * The class or the name of the bean used to configure full text analysis (e.g. analyzers, normalizers).
         */
        @ConfigItem
        public Optional<Class<?>> configurer;
    }
}
