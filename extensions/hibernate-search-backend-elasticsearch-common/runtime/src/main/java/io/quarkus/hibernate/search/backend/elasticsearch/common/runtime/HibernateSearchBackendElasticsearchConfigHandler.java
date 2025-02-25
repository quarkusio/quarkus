package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime;

import static io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil.mergeInto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.engine.cfg.BackendSettings;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchBuildTimeConfig.IndexConfig;

public final class HibernateSearchBackendElasticsearchConfigHandler {

    public static void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> backendConfigs) {
        // We need this weird collecting of names from both @SearchExtension and the configuration properties
        // because a backend/index could potentially be configured exclusively through configuration properties,
        // or exclusively through @SearchExtension.
        // (Well maybe not for backends, but... let's keep it simple.)
        Map<String, Set<String>> backendAndIndexNames = new LinkedHashMap<>();
        mergeInto(backendAndIndexNames, mapperContext.getBackendAndIndexNamesForSearchExtensions());
        for (Entry<String, HibernateSearchBackendElasticsearchBuildTimeConfig> entry : backendConfigs.entrySet()) {
            mergeInto(backendAndIndexNames, entry.getKey(), entry.getValue().indexes().keySet());
        }

        for (Entry<String, Set<String>> entry : backendAndIndexNames.entrySet()) {
            String backendName = entry.getKey();
            Set<String> indexNames = entry.getValue();
            contributeBackendBuildTimeProperties(propertyCollector, mapperContext, backendName, indexNames,
                    backendConfigs.get(backendName));
        }
    }

    private static void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            String backendName, Set<String> indexNames,
            HibernateSearchBackendElasticsearchBuildTimeConfig elasticsearchBackendConfig) {
        addBackendConfig(propertyCollector, backendName, BackendSettings.TYPE,
                ElasticsearchBackendSettings.TYPE_NAME);
        if (elasticsearchBackendConfig != null) {
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION,
                    elasticsearchBackendConfig.version());
        }

        // Settings that may default to a @SearchExtension-annotated-bean
        // <Nothing at the moment>

        // Index defaults at the backend level
        contributeBackendIndexBuildTimeProperties(propertyCollector, mapperContext, backendName, null,
                elasticsearchBackendConfig == null ? null : elasticsearchBackendConfig.indexDefaults());

        // Per-index properties
        for (String indexName : indexNames) {
            IndexConfig indexConfig = elasticsearchBackendConfig == null ? null
                    : elasticsearchBackendConfig.indexes().get(indexName);
            contributeBackendIndexBuildTimeProperties(propertyCollector, mapperContext, backendName, indexName, indexConfig);
        }
    }

    private static void contributeBackendIndexBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            String backendName, String indexName, IndexConfig indexConfig) {
        if (indexConfig != null) {
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
                    indexConfig.schemaManagement().settingsFile());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
                    indexConfig.schemaManagement().mappingFile());
        }

        // Settings that may default to a @SearchExtension-annotated-bean
        addBackendIndexConfig(propertyCollector, backendName, indexName,
                ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
                mapperContext.multiExtensionBeanReferencesFor(
                        indexConfig == null ? Optional.empty() : indexConfig.analysis().configurer(),
                        ElasticsearchAnalysisConfigurer.class, backendName, indexName));
    }

    public static void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            Map<String, HibernateSearchBackendElasticsearchRuntimeConfig> backendConfigs) {
        // We need this weird collecting of names from both @SearchExtension and the configuration properties
        // because a backend/index could potentially be configured exclusively through configuration properties,
        // or exclusively through @SearchExtension.
        // (Well maybe not for backends, but... let's keep it simple.)
        Map<String, Set<String>> backendAndIndexNames = new LinkedHashMap<>();
        mergeInto(backendAndIndexNames, mapperContext.getBackendAndIndexNamesForSearchExtensions());
        for (Entry<String, HibernateSearchBackendElasticsearchRuntimeConfig> entry : backendConfigs.entrySet()) {
            mergeInto(backendAndIndexNames, entry.getKey(), entry.getValue().indexes().keySet());
        }

        for (Entry<String, Set<String>> entry : backendAndIndexNames.entrySet()) {
            String backendName = entry.getKey();
            Set<String> indexNames = entry.getValue();
            contributeBackendRuntimeProperties(propertyCollector, mapperContext, backendName, indexNames,
                    backendConfigs.get(backendName));
        }
    }

    private static void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            String backendName, Set<String> indexNames,
            HibernateSearchBackendElasticsearchRuntimeConfig elasticsearchBackendConfig) {
        if (elasticsearchBackendConfig != null) {
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.HOSTS,
                    elasticsearchBackendConfig.hosts());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.PROTOCOL,
                    elasticsearchBackendConfig.protocol().getHibernateSearchString());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.USERNAME,
                    elasticsearchBackendConfig.username());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.PASSWORD,
                    elasticsearchBackendConfig.password());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.CONNECTION_TIMEOUT,
                    elasticsearchBackendConfig.connectionTimeout().toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.READ_TIMEOUT,
                    elasticsearchBackendConfig.readTimeout().toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.REQUEST_TIMEOUT,
                    elasticsearchBackendConfig.requestTimeout(), Optional::isPresent, d -> d.get().toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS,
                    elasticsearchBackendConfig.maxConnections());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS_PER_ROUTE,
                    elasticsearchBackendConfig.maxConnectionsPerRoute());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.THREAD_POOL_SIZE,
                    elasticsearchBackendConfig.threadPool().size());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION_CHECK_ENABLED,
                    elasticsearchBackendConfig.versionCheck().enabled());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.QUERY_SHARD_FAILURE_IGNORE,
                    elasticsearchBackendConfig.query().shardFailure().ignore());

            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_ENABLED,
                    elasticsearchBackendConfig.discovery().enabled());
            if (elasticsearchBackendConfig.discovery().enabled()) {
                addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL,
                        elasticsearchBackendConfig.discovery().refreshInterval().getSeconds());
            }
        }

        // Settings that may default to a @SearchExtension-annotated-bean
        addBackendConfig(propertyCollector, backendName,
                ElasticsearchBackendSettings.LAYOUT_STRATEGY,
                mapperContext.singleExtensionBeanReferenceFor(
                        elasticsearchBackendConfig == null ? Optional.empty()
                                : elasticsearchBackendConfig.layout().strategy(),
                        IndexLayoutStrategy.class, backendName, null));

        // Index defaults at the backend level
        contributeBackendIndexRuntimeProperties(propertyCollector, mapperContext, backendName, null,
                elasticsearchBackendConfig == null ? null : elasticsearchBackendConfig.indexDefaults());

        // Per-index properties
        for (String indexName : indexNames) {
            HibernateSearchBackendElasticsearchRuntimeConfig.IndexConfig indexConfig = elasticsearchBackendConfig == null ? null
                    : elasticsearchBackendConfig.indexes().get(indexName);
            contributeBackendIndexRuntimeProperties(propertyCollector, mapperContext, backendName, indexName, indexConfig);
        }
    }

    private static void contributeBackendIndexRuntimeProperties(BiConsumer<String, Object> propertyCollector,
            MapperContext mapperContext,
            String backendName, String indexName, HibernateSearchBackendElasticsearchRuntimeConfig.IndexConfig indexConfig) {
        if (indexConfig != null) {
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS,
                    indexConfig.schemaManagement().requiredStatus());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
                    indexConfig.schemaManagement().requiredStatusWaitTimeout(), Optional::isPresent,
                    d -> d.get().toMillis());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_QUEUE_COUNT,
                    indexConfig.indexing().queueCount());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_QUEUE_SIZE,
                    indexConfig.indexing().queueSize());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_MAX_BULK_SIZE,
                    indexConfig.indexing().maxBulkSize());
        }

        // Settings that may default to a @SearchExtension-annotated-bean
        // <Nothing at the moment>
    }
}
