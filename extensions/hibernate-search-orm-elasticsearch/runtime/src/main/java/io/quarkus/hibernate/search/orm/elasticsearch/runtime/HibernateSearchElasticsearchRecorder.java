package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.literal.NamedLiteral;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchPreIntegrationService;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchIndexBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfigPersistenceUnit.ElasticsearchBackendRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfigPersistenceUnit.ElasticsearchIndexRuntimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.management.HibernateSearchManagementHandler;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.mapping.QuarkusHibernateOrmSearchMappingConfigurer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchElasticsearchRecorder {

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(
            String persistenceUnitName, HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
            Set<String> rootAnnotationMappedClassNames,
            List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners) {
        Set<Class<?>> rootAnnotationMappedClasses = new LinkedHashSet<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        for (String className : rootAnnotationMappedClassNames) {
            try {
                rootAnnotationMappedClasses.add(Class.forName(className, true, tccl));
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize mapped class " + className, e);
            }
        }
        return new HibernateSearchIntegrationStaticInitListener(persistenceUnitName,
                buildTimeConfig.persistenceUnits().get(persistenceUnitName),
                backendAndIndexNamesForSearchExtensions, rootAnnotationMappedClasses,
                integrationStaticInitListeners);
    }

    public HibernateOrmIntegrationStaticInitListener createStaticInitInactiveListener() {
        return new HibernateSearchIntegrationStaticInitInactiveListener();
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig, String persistenceUnitName,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
            List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
        HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puConfig = runtimeConfig.persistenceUnits()
                .get(persistenceUnitName);
        return new HibernateSearchIntegrationRuntimeInitListener(persistenceUnitName, puConfig,
                backendAndIndexNamesForSearchExtensions, integrationRuntimeInitListeners);
    }

    public void checkNoExplicitActiveTrue(HibernateSearchElasticsearchRuntimeConfig runtimeConfig) {
        for (var entry : runtimeConfig.persistenceUnits().entrySet()) {
            var config = entry.getValue();
            if (config.active().orElse(false)) {
                var puName = entry.getKey();
                String enabledPropertyKey = HibernateSearchElasticsearchRuntimeConfig.extensionPropertyKey("enabled");
                String activePropertyKey = HibernateSearchElasticsearchRuntimeConfig.mapperPropertyKey(puName, "active");
                throw new ConfigurationException(
                        "Hibernate Search activated explicitly for persistence unit '" + puName
                                + "', but the Hibernate Search extension was disabled at build time."
                                + " If you want Hibernate Search to be active for this persistence unit, you must set '"
                                + enabledPropertyKey
                                + "' to 'true' at build time."
                                + " If you don't want Hibernate Search to be active for this persistence unit, you must leave '"
                                + activePropertyKey
                                + "' unset or set it to 'false'.",
                        Set.of(enabledPropertyKey, activePropertyKey));
            }
        }
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitInactiveListener() {
        return new HibernateSearchIntegrationRuntimeInitInactiveListener();
    }

    public Supplier<SearchMapping> searchMappingSupplier(HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchMapping>() {
            @Override
            public SearchMapping get() {
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puRuntimeConfig = runtimeConfig
                        .persistenceUnits().get(persistenceUnitName);
                if (puRuntimeConfig != null && !puRuntimeConfig.active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchMapping for persistence unit " + persistenceUnitName
                                    + ": Hibernate Search was deactivated through configuration properties");
                }
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

    public Supplier<SearchSession> searchSessionSupplier(HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            String persistenceUnitName, boolean isDefaultPersistenceUnit) {
        return new Supplier<SearchSession>() {
            @Override
            public SearchSession get() {
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit puRuntimeConfig = runtimeConfig
                        .persistenceUnits().get(persistenceUnitName);
                if (puRuntimeConfig != null && !puRuntimeConfig.active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchSession for persistence unit " + persistenceUnitName
                                    + ": Hibernate Search was deactivated through configuration properties");
                }
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

    public Handler<RoutingContext> managementHandler() {
        return new HibernateSearchManagementHandler();
    }

    private static final class HibernateSearchIntegrationStaticInitInactiveListener
            implements HibernateOrmIntegrationStaticInitListener {
        private HibernateSearchIntegrationStaticInitInactiveListener() {
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

        private final String persistenceUnitName;
        private final HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig;
        private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;
        private final Set<Class<?>> rootAnnotationMappedClasses;
        private final List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners;

        private HibernateSearchIntegrationStaticInitListener(String persistenceUnitName,
                HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig,
                Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
                Set<Class<?>> rootAnnotationMappedClasses,
                List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners) {
            this.persistenceUnitName = persistenceUnitName;
            this.buildTimeConfig = buildTimeConfig;
            this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
            this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
            this.integrationStaticInitListeners = integrationStaticInitListeners;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    EngineSettings.BACKGROUND_FAILURE_HANDLER,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty() : buildTimeConfig.backgroundFailureHandler(),
                            FailureHandler.class, persistenceUnitName, null, null));

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.MAPPING_CONFIGURER,
                    collectAllHibernateOrmSearchMappingConfigurers());

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.COORDINATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty() : buildTimeConfig.coordination().strategy(),
                            CoordinationStrategy.class, persistenceUnitName, null, null));

            // We need this weird collecting of names from both @SearchExtension and the configuration properties
            // because a backend/index could potentially be configured exclusively through configuration properties,
            // or exclusively through @SearchExtension.
            // (Well maybe not for backends, but... let's keep it simple.)
            Map<String, ElasticsearchBackendBuildTimeConfig> backendConfigs = buildTimeConfig == null
                    ? Collections.emptyMap()
                    : buildTimeConfig.backends();
            Map<String, Set<String>> backendAndIndexNames = new LinkedHashMap<>();
            mergeInto(backendAndIndexNames, backendAndIndexNamesForSearchExtensions);
            for (Entry<String, ElasticsearchBackendBuildTimeConfig> entry : backendConfigs.entrySet()) {
                mergeInto(backendAndIndexNames, entry.getKey(), entry.getValue().indexes().keySet());
            }

            for (Entry<String, Set<String>> entry : backendAndIndexNames.entrySet()) {
                String backendName = entry.getKey();
                Set<String> indexNames = entry.getValue();
                contributeBackendBuildTimeProperties(propertyCollector, backendName, indexNames,
                        backendConfigs.get(backendName));
            }

            for (HibernateOrmIntegrationStaticInitListener listener : integrationStaticInitListeners) {
                listener.contributeBootProperties(propertyCollector);
            }
        }

        private List<BeanReference<HibernateOrmSearchMappingConfigurer>> collectAllHibernateOrmSearchMappingConfigurers() {
            List<BeanReference<HibernateOrmSearchMappingConfigurer>> configurers = new ArrayList<>();
            // 1. We add the quarkus-specific configurer:
            configurers
                    .add(BeanReference.ofInstance(new QuarkusHibernateOrmSearchMappingConfigurer(rootAnnotationMappedClasses)));
            // 2. Then we check if any configurers were supplied by a user be it through a property or via an extension:
            Optional<List<BeanReference<HibernateOrmSearchMappingConfigurer>>> beanReferences = HibernateSearchBeanUtil
                    .multiExtensionBeanReferencesFor(
                            buildTimeConfig.mapping().configurer(),
                            HibernateOrmSearchMappingConfigurer.class,
                            persistenceUnitName, null, null);
            if (beanReferences.isPresent()) {
                configurers.addAll(beanReferences.get());
            }

            return configurers;
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
            HibernateOrmIntegrationBooter booter = HibernateOrmIntegrationBooter.builder(metadata, bootstrapContext)
                    // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21
                    .valueReadHandleFactory(ValueHandleFactory.usingJavaLangReflect())
                    .build();
            booter.preBoot(propertyCollector);

            for (HibernateOrmIntegrationStaticInitListener listener : integrationStaticInitListeners) {
                listener.onMetadataInitialized(metadata, bootstrapContext, propertyCollector);
            }
        }

        private void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                Set<String> indexNames,
                ElasticsearchBackendBuildTimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, BackendSettings.TYPE,
                    ElasticsearchBackendSettings.TYPE_NAME);
            if (elasticsearchBackendConfig != null) {
                addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION,
                        elasticsearchBackendConfig.version());
            }

            // Settings that may default to a @SearchExtension-annotated-bean
            addBackendConfig(propertyCollector, backendName,
                    ElasticsearchBackendSettings.LAYOUT_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            elasticsearchBackendConfig == null ? Optional.empty()
                                    : elasticsearchBackendConfig.layout().strategy(),
                            IndexLayoutStrategy.class, persistenceUnitName, backendName, null));

            // Index defaults at the backend level
            contributeBackendIndexBuildTimeProperties(propertyCollector, backendName, null,
                    elasticsearchBackendConfig == null ? null : elasticsearchBackendConfig.indexDefaults());

            // Per-index properties
            for (String indexName : indexNames) {
                ElasticsearchIndexBuildTimeConfig indexConfig = elasticsearchBackendConfig == null ? null
                        : elasticsearchBackendConfig.indexes().get(indexName);
                contributeBackendIndexBuildTimeProperties(propertyCollector, backendName, indexName, indexConfig);
            }
        }

        private void contributeBackendIndexBuildTimeProperties(BiConsumer<String, Object> propertyCollector,
                String backendName, String indexName, ElasticsearchIndexBuildTimeConfig indexConfig) {
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
                    HibernateSearchBeanUtil.multiExtensionBeanReferencesFor(
                            indexConfig == null ? Optional.empty() : indexConfig.analysis().configurer(),
                            ElasticsearchAnalysisConfigurer.class, persistenceUnitName, backendName, indexName));
        }
    }

    private static final class HibernateSearchIntegrationRuntimeInitInactiveListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private HibernateSearchIntegrationRuntimeInitInactiveListener() {
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            // Not strictly necessary since this should be set during static init,
            // but let's be on the safe side.
            propertyCollector.accept(HibernateOrmMapperSettings.ENABLED, false);
        }

        @Override
        public List<StandardServiceInitiator<?>> contributeServiceInitiators() {
            return List.of(
                    // The service must be initiated even if Hibernate Search is not supposed to start,
                    // because it's also responsible for determining that Hibernate Search should not start.
                    new HibernateSearchPreIntegrationService.Initiator());
        }
    }

    private static final class HibernateSearchIntegrationRuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {

        private final String persistenceUnitName;
        private final HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig;
        private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;
        private final List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners;

        private HibernateSearchIntegrationRuntimeInitListener(String persistenceUnitName,
                HibernateSearchElasticsearchRuntimeConfigPersistenceUnit runtimeConfig,
                Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
                List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners) {
            this.persistenceUnitName = persistenceUnitName;
            this.runtimeConfig = runtimeConfig;
            this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
            this.integrationRuntimeInitListeners = integrationRuntimeInitListeners;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig != null) {
                if (!runtimeConfig.active().orElse(true)) {
                    addConfig(propertyCollector, HibernateOrmMapperSettings.ENABLED, false);
                    // Do not process other properties: Hibernate Search is disabled anyway.
                    return;
                }

                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
                        runtimeConfig.schemaManagement().strategy());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK,
                        runtimeConfig.automaticIndexing().enableDirtyCheck());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.QUERY_LOADING_CACHE_LOOKUP_STRATEGY,
                        runtimeConfig.queryLoading().cacheLookup().strategy());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.QUERY_LOADING_FETCH_SIZE,
                        runtimeConfig.queryLoading().fetchSize());
                addConfig(propertyCollector,
                        HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS,
                        runtimeConfig.multiTenancy().tenantIds());
            }

            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.indexing().plan().synchronization().strategy(),
                            IndexingPlanSynchronizationStrategy.class, persistenceUnitName, null, null));
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.automaticIndexing().synchronization().strategy(),
                            AutomaticIndexingSynchronizationStrategy.class, persistenceUnitName, null, null));

            // We need this weird collecting of names from both @SearchExtension and the configuration properties
            // because a backend/index could potentially be configured exclusively through configuration properties,
            // or exclusively through @SearchExtension.
            // (Well maybe not for backends, but... let's keep it simple.)
            Map<String, ElasticsearchBackendRuntimeConfig> backendConfigs = runtimeConfig == null
                    ? Collections.emptyMap()
                    : runtimeConfig.backends();
            Map<String, Set<String>> backendAndIndexNames = new LinkedHashMap<>();
            mergeInto(backendAndIndexNames, backendAndIndexNamesForSearchExtensions);
            for (Entry<String, ElasticsearchBackendRuntimeConfig> entry : backendConfigs.entrySet()) {
                mergeInto(backendAndIndexNames, entry.getKey(), entry.getValue().indexes().keySet());
            }

            for (Entry<String, Set<String>> entry : backendAndIndexNames.entrySet()) {
                String backendName = entry.getKey();
                Set<String> indexNames = entry.getValue();
                contributeBackendRuntimeProperties(propertyCollector, backendName, indexNames,
                        backendConfigs.get(backendName));
            }

            for (HibernateOrmIntegrationRuntimeInitListener integrationRuntimeInitListener : integrationRuntimeInitListeners) {
                integrationRuntimeInitListener.contributeRuntimeProperties(propertyCollector);
            }
        }

        private void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                Set<String> indexNames, ElasticsearchBackendRuntimeConfig elasticsearchBackendConfig) {
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
            // <Nothing at the moment>

            // Index defaults at the backend level
            contributeBackendIndexRuntimeProperties(propertyCollector, backendName, null,
                    elasticsearchBackendConfig == null ? null : elasticsearchBackendConfig.indexDefaults());

            // Per-index properties
            for (String indexName : indexNames) {
                ElasticsearchIndexRuntimeConfig indexConfig = elasticsearchBackendConfig == null ? null
                        : elasticsearchBackendConfig.indexes().get(indexName);
                contributeBackendIndexRuntimeProperties(propertyCollector, backendName, indexName, indexConfig);
            }
        }

        private void contributeBackendIndexRuntimeProperties(BiConsumer<String, Object> propertyCollector,
                String backendName, String indexName, ElasticsearchIndexRuntimeConfig indexConfig) {
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

        @Override
        public List<StandardServiceInitiator<?>> contributeServiceInitiators() {
            return List.of(
                    // One of the purposes of this service is to provide configuration to Hibernate Search,
                    // so it absolutely must be updated with the runtime configuration.
                    // The service must be initiated even if Hibernate Search is not supposed to start,
                    // because it's also responsible for determining that Hibernate Search should not start.
                    new HibernateSearchPreIntegrationService.Initiator());
        }
    }

    private static void mergeInto(Map<String, Set<String>> target, Map<String, Set<String>> source) {
        for (Entry<String, Set<String>> entry : source.entrySet()) {
            mergeInto(target, entry.getKey(), entry.getValue());
        }
    }

    private static void mergeInto(Map<String, Set<String>> target, String key, Set<String> values) {
        target.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                .addAll(values);
    }
}
