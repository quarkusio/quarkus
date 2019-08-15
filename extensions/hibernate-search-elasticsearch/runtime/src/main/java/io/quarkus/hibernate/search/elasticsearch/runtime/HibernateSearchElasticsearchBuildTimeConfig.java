package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HibernateSearchElasticsearchBuildTimeConfig {

    /**
     * Configuration of the default backend.
     */
    public ElasticsearchBackendBuildTimeConfig elasticsearch;

    /**
     * If not using the default backend configuration, the name of the default backend that is part of the
     * {@link #additionalBackends}.
     */
    @ConfigItem
    public Optional<String> defaultBackend;

    /**
     * Configuration of optional additional backends.
     */
    @ConfigItem(name = "elasticsearch.backends")
    public Map<String, ElasticsearchBackendBuildTimeConfig> additionalBackends;

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
         * The class or the name of the bean used to configure full text analysis (e.g. analyzers, normalizers).
         */
        @ConfigItem
        public Optional<Class<?>> analysisConfigurer;
    }
}
