package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchConfigUtil.addConfig;

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
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooter;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig.ElasticsearchIndexBuildTimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.ElasticsearchBackendRuntimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.ElasticsearchIndexRuntimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean.ArcBeanProvider;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.management.HibernateSearchStandaloneManagementHandler;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.mapping.QuarkusHibernateSearchStandaloneMappingConfigurer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HibernateSearchStandaloneRecorder {

    public void preBoot(HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
            Set<String> rootAnnotationMappedClassNames) {
        Set<Class<?>> rootAnnotationMappedClasses = new LinkedHashSet<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        for (String className : rootAnnotationMappedClassNames) {
            try {
                rootAnnotationMappedClasses.add(Class.forName(className, true, tccl));
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize mapped class " + className, e);
            }
        }
        Map<String, Object> bootProperties = new LinkedHashMap<>();
        new StaticInitListener(buildTimeConfig, backendAndIndexNamesForSearchExtensions, rootAnnotationMappedClasses)
                .contributeBootProperties(bootProperties::put);
        StandalonePojoIntegrationBooter booter = StandalonePojoIntegrationBooter.builder()
                .properties(bootProperties)
                // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21
                .valueReadHandleFactory(ValueHandleFactory.usingJavaLangReflect())
                // Integrate CDI
                .property(StandalonePojoMapperSpiSettings.BEAN_PROVIDER, new ArcBeanProvider(Arc.container()))
                .build();
        booter.preBoot(bootProperties::put);
        HibernateSearchStandalonePreBootState.set(bootProperties);
    }

    public void checkNoExplicitActiveTrue(HibernateSearchStandaloneRuntimeConfig runtimeConfig) {
        if (runtimeConfig.active().orElse(false)) {
            String enabledPropertyKey = HibernateSearchStandaloneRuntimeConfig.extensionPropertyKey("enabled");
            String activePropertyKey = HibernateSearchStandaloneRuntimeConfig.mapperPropertyKey("active");
            throw new ConfigurationException(
                    "Hibernate Search Standalone activated explicitly,"
                            + " but the Hibernate Search Standalone extension was disabled at build time."
                            + " If you want Hibernate Search Standalone to be active, you must set '"
                            + enabledPropertyKey
                            + "' to 'true' at build time."
                            + " If you don't want Hibernate Search Standalone to be active, you must leave '"
                            + activePropertyKey
                            + "' unset or set it to 'false'.",
                    Set.of(enabledPropertyKey, activePropertyKey));
        }
    }

    public void clearPreBootState() {
        HibernateSearchStandalonePreBootState.pop();
    }

    public Function<SyntheticCreationalContext<SearchMapping>, SearchMapping> createSearchMappingFunction(
            HibernateSearchStandaloneRuntimeConfig runtimeConfig,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
        return new Function<SyntheticCreationalContext<SearchMapping>, SearchMapping>() {
            @Override
            public SearchMapping apply(SyntheticCreationalContext<SearchMapping> context) {
                if (runtimeConfig != null && !runtimeConfig.active().orElse(true)) {
                    throw new IllegalStateException(
                            "Cannot retrieve the SearchMapping: Hibernate Search Standalone was deactivated through configuration properties");
                }
                Map<String, Object> bootProperties = new LinkedHashMap<>(HibernateSearchStandalonePreBootState.pop());
                new RuntimeInitListener(runtimeConfig, backendAndIndexNamesForSearchExtensions)
                        .contributeRuntimeProperties(bootProperties::put);
                StandalonePojoIntegrationBooter booter = StandalonePojoIntegrationBooter.builder()
                        .properties(bootProperties)
                        .build();
                return booter.boot();
            }
        };
    }

    public void bootEagerly(HibernateSearchStandaloneRuntimeConfig runtimeConfig) {
        if (runtimeConfig != null && !runtimeConfig.active().orElse(true)) {
            // Hibernate Search is deactivated: skip eager bootstrap.
            return;
        }
        Arc.container().instance(SearchMapping.class).get()
                // Just call some side-effect-free method to initialize the proxy
                .allIndexedEntities();
    }

    public Handler<RoutingContext> managementHandler() {
        return new HibernateSearchStandaloneManagementHandler();
    }

    private static final class StaticInitListener {
        private final HibernateSearchStandaloneBuildTimeConfig buildTimeConfig;
        private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;
        private final Set<Class<?>> rootAnnotationMappedClasses;

        private StaticInitListener(HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
                Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
                Set<Class<?>> rootAnnotationMappedClasses) {
            this.buildTimeConfig = buildTimeConfig;
            this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
            this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
        }

        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    EngineSettings.BACKGROUND_FAILURE_HANDLER,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            buildTimeConfig == null ? Optional.empty()
                                    : buildTimeConfig.backgroundFailureHandler(),
                            FailureHandler.class, null, null));

            addConfig(propertyCollector,
                    StandalonePojoMapperSettings.MAPPING_CONFIGURER,
                    collectAllStandalonePojoMappingConfigurers());

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
        }

        private List<BeanReference<StandalonePojoMappingConfigurer>> collectAllStandalonePojoMappingConfigurers() {
            List<BeanReference<StandalonePojoMappingConfigurer>> configurers = new ArrayList<>();
            // 1. We add the quarkus-specific configurer:
            configurers.add(BeanReference.ofInstance(
                    new QuarkusHibernateSearchStandaloneMappingConfigurer(buildTimeConfig.mapping().structure(),
                            rootAnnotationMappedClasses)));
            // 2. Then we check if any configurers were supplied by a user be it through a property or via an extension:
            Optional<List<BeanReference<StandalonePojoMappingConfigurer>>> beanReferences = HibernateSearchBeanUtil
                    .multiExtensionBeanReferencesFor(
                            buildTimeConfig.mapping().configurer(),
                            StandalonePojoMappingConfigurer.class, null, null);
            if (beanReferences.isPresent()) {
                configurers.addAll(beanReferences.get());
            }

            return configurers;
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
                            indexConfig == null ? Optional.empty()
                                    : indexConfig.analysis().configurer(),
                            ElasticsearchAnalysisConfigurer.class, backendName,
                            indexName));
        }
    }

    private static final class RuntimeInitListener {
        private final HibernateSearchStandaloneRuntimeConfig runtimeConfig;
        private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;

        private RuntimeInitListener(HibernateSearchStandaloneRuntimeConfig runtimeConfig,
                Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
            this.runtimeConfig = runtimeConfig;
            this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
        }

        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig != null) {
                addConfig(propertyCollector,
                        StandalonePojoMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
                        runtimeConfig.schemaManagement().strategy());
            }

            addConfig(propertyCollector,
                    StandalonePojoMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
                    HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                            runtimeConfig == null ? Optional.empty()
                                    : runtimeConfig.indexing().plan().synchronization().strategy(),
                            IndexingPlanSynchronizationStrategy.class, null, null));

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

                // Settings that may default to a @SearchExtension-annotated-bean
                addBackendConfig(propertyCollector, backendName,
                        ElasticsearchBackendSettings.LAYOUT_STRATEGY,
                        HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(
                                elasticsearchBackendConfig.layout().strategy(),
                                IndexLayoutStrategy.class, backendName, null));
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
