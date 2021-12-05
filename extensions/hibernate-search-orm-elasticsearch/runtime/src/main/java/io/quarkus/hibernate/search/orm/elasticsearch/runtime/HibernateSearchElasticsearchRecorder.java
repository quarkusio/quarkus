package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.enterprise.inject.literal.NamedLiteral;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchIndexBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfigPersistenceUnit.ElasticsearchBackendRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfigPersistenceUnit.ElasticsearchIndexRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateSearchElasticsearchRecorder {

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig) {
        return new HibernateSearchIntegrationStaticInitListener(buildTimeConfig);
    }

    public HibernateOrmIntegrationStaticInitListener createDisabledListener() {
        return new HibernateSearchIntegrationDisabledListener();
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig, String persistenceUnitName,
            List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
        HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puConfig = PersistenceUnitUtil
                .isDefaultPersistenceUnit(persistenceUnitName)
                        ? runtimeConfig.defaultPersistenceUnit
                        : runtimeConfig.persistenceUnits.get(persistenceUnitName);
        if (puConfig == null) {
            return null;
        }
        return new HibernateSearchIntegrationRuntimeInitListener(puConfig, integrationRuntimeInitListeners);
    }

    public Supplier<SearchMapping> searchMappingSupplier(String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchMapping>() {
            @Override
            public SearchMapping get() {
                SessionFactory sessionFactory;
                if (isDefaultPersistenceUnit) {
                    sessionFactory = Arc.container().instance(SessionFactory.class).get();
                } else {
                    sessionFactory = Arc.container().instance(
                            SessionFactory.class, NamedLiteral.of(persistenceUnitName)).get();
                }
                return Search.mapping(sessionFactory);
            }
        };
    }

    public Supplier<SearchSession> searchSessionSupplier(String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchSession>() {
            @Override
            public SearchSession get() {
                Session session;
                if (isDefaultPersistenceUnit) {
                    session = Arc.container().instance(Session.class).get();
                } else {
                    session = Arc.container().instance(
                            Session.class, NamedLiteral.of(persistenceUnitName)).get();
                }
                return Search.session(session);
            }
        };
    }

    private static final class HibernateSearchIntegrationDisabledListener
            implements HibernateOrmIntegrationStaticInitListener {
        private HibernateSearchIntegrationDisabledListener() {
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            propertyCollector.accept(HibernateOrmMapperSettings.ENABLED, false);
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }

    private static final class HibernateSearchIntegrationStaticInitListener
            implements HibernateOrmIntegrationStaticInitListener {

        private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;

        private HibernateSearchIntegrationStaticInitListener(
                HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
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
            HibernateOrmIntegrationBooter booter = HibernateOrmIntegrationBooter.builder(metadata, bootstrapContext)
                    // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21
                    .valueReadHandleFactory(ValueReadHandleFactory.usingJavaLangReflect())
                    .build();
            booter.preBoot(propertyCollector);
        }

        private void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendBuildTimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, BackendSettings.TYPE,
                    ElasticsearchBackendSettings.TYPE_NAME);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION,
                    elasticsearchBackendConfig.version);
            addBackendConfig(propertyCollector, backendName,
                    ElasticsearchBackendSettings.LAYOUT_STRATEGY,
                    elasticsearchBackendConfig.layout.strategy);

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

        private void contributeBackendIndexBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
                String backendName, String indexName, ElasticsearchIndexBuildTimeConfig indexConfig) {
            addBackendIndexConfig(propertyCollector, backendName, indexName,
                    ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
                    indexConfig.analysis.configurer);
        }
    }

    private static final class HibernateSearchIntegrationRuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private final HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig;
        private final List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners;

        private HibernateSearchIntegrationRuntimeInitListener(
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig,
                List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
            this.runtimeConfig = runtimeConfig;
            this.integrationRuntimeInitListeners = integrationRuntimeInitListeners;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (!runtimeConfig.enabled) {
                addConfig(propertyCollector, HibernateOrmMapperSettings.ENABLED, false);
                // Do not process other properties: Hibernate Search is disabled anyway.
                return;
            }

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

            contributeBackendRuntimeProperties(propertyCollector, null,
                    runtimeConfig.defaultBackend);

            for (Entry<String, ElasticsearchBackendRuntimeConfig> backendEntry : runtimeConfig.namedBackends.backends
                    .entrySet()) {
                contributeBackendRuntimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }

            for (HibernateOrmIntegrationRuntimeInitListener integrationRuntimeInitListener : integrationRuntimeInitListeners) {
                integrationRuntimeInitListener.contributeRuntimeProperties(propertyCollector);
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
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION_CHECK_ENABLED,
                    elasticsearchBackendConfig.versionCheck);

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
