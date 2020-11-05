package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmReflectionStrategyName;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrations;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchIndexBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.ElasticsearchBackendRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.ElasticsearchIndexRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchElasticsearchRecorder {

    private static HibernateSearchElasticsearchRuntimeConfig runtimeConfig;

    public void registerHibernateSearchIntegration(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
        HibernateOrmIntegrations.registerListener(new HibernateSearchIntegrationListener(buildTimeConfig));
    }

    public void setRuntimeConfig(HibernateSearchElasticsearchRuntimeConfig runtimeConfig) {
        HibernateSearchElasticsearchRecorder.runtimeConfig = runtimeConfig;
    }

    private static final class HibernateSearchIntegrationListener implements HibernateOrmIntegrationListener {

        private final HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

        private HibernateSearchIntegrationListener(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, HibernateOrmMapperSpiSettings.REFLECTION_STRATEGY,
                    HibernateOrmReflectionStrategyName.JAVA_LANG_REFLECT);

            addConfig(propertyCollector,
                    EngineSettings.BACKGROUND_FAILURE_HANDLER,
                    buildTimeConfig.backgroundFailureHandler);

            contributeBackendBuildTimeProperties(propertyCollector, null, buildTimeConfig.defaultBackend);

            for (Entry<String, ElasticsearchBackendBuildTimeConfig> backendEntry : buildTimeConfig.namedBackends.backends
                    .entrySet()) {
                contributeBackendBuildTimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
            HibernateOrmIntegrationBooter booter = HibernateOrmIntegrationBooter.create(metadata, bootstrapContext);
            booter.preBoot(propertyCollector);
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
                    runtimeConfig.schemaManagement.strategy);
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
                    runtimeConfig.automaticIndexing.synchronization.strategy);
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK,
                    runtimeConfig.automaticIndexing.enableDirtyCheck);
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.QUERY_LOADING_CACHE_LOOKUP_STRATEGY,
                    runtimeConfig.queryLoading.cacheLookup.strategy);
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE,
                    runtimeConfig.queryLoading.fetchSize);

            contributeBackendRuntimeProperties(propertyCollector, null, runtimeConfig.defaultBackend);

            for (Entry<String, ElasticsearchBackendRuntimeConfig> backendEntry : runtimeConfig.namedBackends.backends
                    .entrySet()) {
                contributeBackendRuntimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }
        }

        private void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendBuildTimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, BackendSettings.TYPE,
                    ElasticsearchBackendSettings.TYPE_NAME);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION,
                    elasticsearchBackendConfig.version);
            addBackendConfig(propertyCollector, backendName,
                    ElasticsearchBackendSettings.LAYOUT_STRATEGY,
                    elasticsearchBackendConfig.layout.strategy,
                    Optional::isPresent, c -> c.get().getName());

            // Index defaults at the backend level
            contributeBackendIndexBuildTimeProperties(propertyCollector, backendName, null,
                    elasticsearchBackendConfig.indexDefaults);

            // Per-index properties
            for (Entry<String, ElasticsearchIndexBuildTimeConfig> indexConfigEntry : elasticsearchBackendConfig.indexes
                    .entrySet()) {
                String indexName = indexConfigEntry.getKey();
                ElasticsearchIndexBuildTimeConfig indexConfig = indexConfigEntry.getValue();
                contributeBackendIndexBuildTimeProperties(propertyCollector, backendName, indexName, indexConfig);
            }
        }

        private void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendRuntimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.HOSTS,
                    elasticsearchBackendConfig.hosts);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.PROTOCOL,
                    elasticsearchBackendConfig.protocol.getHibernateSearchString());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.USERNAME,
                    elasticsearchBackendConfig.username);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.PASSWORD,
                    elasticsearchBackendConfig.password);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.CONNECTION_TIMEOUT,
                    elasticsearchBackendConfig.connectionTimeout.toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.READ_TIMEOUT,
                    elasticsearchBackendConfig.readTimeout.toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.REQUEST_TIMEOUT,
                    elasticsearchBackendConfig.requestTimeout, Optional::isPresent, d -> d.get().toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS,
                    elasticsearchBackendConfig.maxConnections);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS_PER_ROUTE,
                    elasticsearchBackendConfig.maxConnectionsPerRoute);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.THREAD_POOL_SIZE,
                    elasticsearchBackendConfig.threadPool.size);

            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_ENABLED,
                    elasticsearchBackendConfig.discovery.enabled);
            if (elasticsearchBackendConfig.discovery.enabled) {
                addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL,
                        elasticsearchBackendConfig.discovery.refreshInterval.getSeconds());
            }

            // Index defaults at the backend level
            contributeBackendIndexRuntimeProperties(propertyCollector, backendName, null,
                    elasticsearchBackendConfig.indexDefaults);

            // Per-index properties
            for (Entry<String, ElasticsearchIndexRuntimeConfig> indexConfigEntry : runtimeConfig.defaultBackend.indexes
                    .entrySet()) {
                String indexName = indexConfigEntry.getKey();
                ElasticsearchIndexRuntimeConfig indexConfig = indexConfigEntry.getValue();
                contributeBackendIndexRuntimeProperties(propertyCollector, backendName, indexName, indexConfig);
            }
        }

        private void contributeBackendIndexBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
                String backendName, String indexName, ElasticsearchIndexBuildTimeConfig indexConfig) {
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
                    indexConfig.analysis.configurer,
                    Optional::isPresent, c -> c.get().getName());
        }

        private void contributeBackendIndexRuntimeProperties(BiConsumer<String, Object> propertyCollector,
                String backendName, String indexName, ElasticsearchIndexRuntimeConfig indexConfig) {
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS,
                    indexConfig.schemaManagement.requiredStatus);
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
                    indexConfig.schemaManagement.requiredStatusWaitTimeout, Optional::isPresent,
                    d -> d.get().toMillis());
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_QUEUE_COUNT,
                    indexConfig.indexing.queueCount);
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_QUEUE_SIZE,
                    indexConfig.indexing.queueSize);
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.INDEXING_MAX_BULK_SIZE,
                    indexConfig.indexing.maxBulkSize);
        }
    }
}
