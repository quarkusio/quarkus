package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.ClassNames.INDEXED;
import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.ClassNames.PROJECTION_CONSTRUCTOR;
import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.ClassNames.ROOT_MAPPING;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.backendPropertyKey;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.defaultBackendPropertyKeys;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.mapperPropertyKey;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.mapperPropertyKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.search.backend.elasticsearch.common.deployment.HibernateSearchBackendElasticsearchEnabledBuildItem;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.ElasticsearchVersionSubstitution;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchOrmElasticsearchMapperContext;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

@BuildSteps(onlyIf = HibernateSearchEnabled.class)
class HibernateSearchElasticsearchProcessor {

    static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search ORM + Elasticsearch";

    private static final Logger LOG = Logger.getLogger(HibernateSearchElasticsearchProcessor.class);

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);

        Map<String, Map<String, Set<String>>> persistenceUnitAndBackendAndIndexNamesForSearchExtensions = collectPersistenceUnitAndBackendAndIndexNamesForSearchExtensions(
                index);

        Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> configByPU = buildTimeConfig
                .persistenceUnits();

        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            Collection<AnnotationInstance> indexedAnnotationsForPU = new ArrayList<>();
            for (AnnotationInstance indexedAnnotation : indexedAnnotations) {
                String targetName = indexedAnnotation.target().asClass().name().toString();
                if (puDescriptor.getManagedClassNames().contains(targetName)) {
                    indexedAnnotationsForPU.add(indexedAnnotation);
                }
            }
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions = persistenceUnitAndBackendAndIndexNamesForSearchExtensions
                    .getOrDefault(puDescriptor.getPersistenceUnitName(), Collections.emptyMap());
            String puName = puDescriptor.getPersistenceUnitName();
            buildForPersistenceUnit(recorder, indexedAnnotationsForPU, puName, configByPU.get(puName),
                    backendAndIndexNamesForSearchExtensions,
                    configuredPersistenceUnits, staticIntegrations, runtimeIntegrations);
        }
    }

    private static Map<String, Map<String, Set<String>>> collectPersistenceUnitAndBackendAndIndexNamesForSearchExtensions(
            IndexView index) {
        Map<String, Map<String, Set<String>>> result = new LinkedHashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(ClassNames.SEARCH_EXTENSION)) {
            var puName = annotation.value("persistenceUnit");
            var backendName = annotation.value("backend");
            var indexName = annotation.value("index");
            Set<String> indexNames = result
                    .computeIfAbsent(puName == null ? PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME : puName.asString(),
                            ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(backendName == null ? null : backendName.asString(), ignored -> new LinkedHashSet<>());
            if (indexName != null) {
                indexNames.add(indexName.asString());
            }
        }
        return result;
    }

    private void buildForPersistenceUnit(HibernateSearchElasticsearchRecorder recorder,
            Collection<AnnotationInstance> indexedAnnotationsForPU, String persistenceUnitName,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit puConfig,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations) {
        if (indexedAnnotationsForPU.isEmpty()) {
            // we don't have any indexed entity, we can disable Hibernate Search
            staticIntegrations.produce(new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    persistenceUnitName).setInitListener(recorder.createStaticInitInactiveListener()));
            // we need a runtime listener even when Hibernate Search is disabled,
            // just to let Hibernate Search boot up until the point where it checks whether it's enabled or not
            runtimeIntegrations.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    persistenceUnitName)
                    .setInitListener(recorder.createRuntimeInitInactiveListener()));
            return;
        }

        Set<String> backendNamesForIndexedEntities = new LinkedHashSet<>();
        for (AnnotationInstance indexedAnnotation : indexedAnnotationsForPU) {
            AnnotationValue backendNameValue = indexedAnnotation.value("backend");
            String backendName = backendNameValue == null ? null : backendNameValue.asString();
            backendNamesForIndexedEntities.add(backendName);
        }

        configuredPersistenceUnits
                .produce(new HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(
                        new HibernateSearchOrmElasticsearchMapperContext(persistenceUnitName,
                                backendNamesForIndexedEntities, backendAndIndexNamesForSearchExtensions),
                        puConfig));
    }

    @BuildStep
    void enableBackend(List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> enabledPUs,
            BuildProducer<HibernateSearchBackendElasticsearchEnabledBuildItem> elasticsearchEnabled) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem enabled : enabledPUs) {
            var buildTimeConfig = enabled.getBuildTimeConfig();
            elasticsearchEnabled.produce(new HibernateSearchBackendElasticsearchEnabledBuildItem(enabled.mapperContext,
                    buildTimeConfig == null ? Collections.emptyMap() : buildTimeConfig.backends()));
        }
    }

    @BuildStep
    void registerBeans(List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> searchEnabledPUs,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        if (searchEnabledPUs.isEmpty()) {
            return;
        }

        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(FailureHandler.class,
                AutomaticIndexingSynchronizationStrategy.class, IndexingPlanSynchronizationStrategy.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setStaticConfig(RecorderContext recorderContext, HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<HibernateSearchIntegrationStaticConfiguredBuildItem> integrationStaticConfigBuildItems,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticConfigured) {
        // Make it possible to record the settings as bytecode:
        recorderContext.registerSubstitution(ElasticsearchVersion.class,
                String.class, ElasticsearchVersionSubstitution.class);

        IndexView index = combinedIndexBuildItem.getIndex();
        Set<String> rootAnnotationMappedClassNames = collectRootAnnotationMappedClassNames(index);

        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            List<HibernateOrmIntegrationStaticInitListener> integrationStaticInitListeners = new ArrayList<>();
            boolean xmlMappingRequired = false;
            for (HibernateSearchIntegrationStaticConfiguredBuildItem item : integrationStaticConfigBuildItems) {
                if (item.getPersistenceUnitName().equals(puName)) {
                    HibernateOrmIntegrationStaticInitListener listener = item.getInitListener();
                    if (listener != null) {
                        integrationStaticInitListeners.add(listener);
                    }
                }
                if (item.isXmlMappingRequired()) {
                    xmlMappingRequired = true;
                }
            }
            staticConfigured.produce(
                    new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH, puName)
                            .setInitListener(
                                    // we cannot pass a config group to a recorder so passing the whole config
                                    recorder.createStaticInitListener(
                                            configuredPersistenceUnit.mapperContext,
                                            rootAnnotationMappedClassNames,
                                            integrationStaticInitListeners))
                            .setXmlMappingRequired(xmlMappingRequired));
        }
    }

    private static Set<String> collectRootAnnotationMappedClassNames(IndexView index) {
        // Look for classes annotated with annotations meta-annotated with @RootMapping:
        // those classes will have their annotations processed on every persistence unit.
        // At the moment only @ProjectionConstructor is meta-annotated with @RootMapping.
        Set<DotName> rootMappingAnnotationNames = new LinkedHashSet<>();
        // This is built into Hibernate Search, which may not be part of the index.
        rootMappingAnnotationNames.add(PROJECTION_CONSTRUCTOR);
        // Users can theoretically declare their own root mapping annotations
        // (replacements for @ProjectionConstructor, for example),
        // so we need to consider those as well.
        for (AnnotationInstance rootMappingAnnotationInstance : index.getAnnotations(ROOT_MAPPING)) {
            rootMappingAnnotationNames.add(rootMappingAnnotationInstance.target().asClass().name());
        }

        // We'll collect all classes annotated with "root mapping" annotations
        // anywhere (type level, constructor, ...)
        Set<String> rootAnnotationMappedClassNames = new LinkedHashSet<>();
        for (DotName rootMappingAnnotationName : rootMappingAnnotationNames) {
            for (AnnotationInstance annotation : index.getAnnotations(rootMappingAnnotationName)) {
                rootAnnotationMappedClassNames.add(JandexUtil.getEnclosingClass(annotation).name().toString());
            }
        }
        return rootAnnotationMappedClassNames;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchElasticsearchRecorder recorder,
            List<HibernateSearchIntegrationRuntimeConfiguredBuildItem> integrationRuntimeConfigBuildItems,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            List<HibernateOrmIntegrationRuntimeInitListener> integrationRuntimeInitListeners = new ArrayList<>();
            for (HibernateSearchIntegrationRuntimeConfiguredBuildItem item : integrationRuntimeConfigBuildItems) {
                if (item.getPersistenceUnitName().equals(puName)) {
                    HibernateOrmIntegrationRuntimeInitListener listener = item.getInitListener();
                    if (listener != null) {
                        integrationRuntimeInitListeners.add(listener);
                    }
                }
            }
            runtimeConfigured.produce(
                    new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH, puName)
                            .setInitListener(recorder.createRuntimeInitListener(configuredPersistenceUnit.mapperContext,
                                    integrationRuntimeInitListeners)));
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    DevservicesElasticsearchBuildItem devServices(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
        var defaultPUConfig = buildTimeConfig.persistenceUnits().get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        if (defaultPUConfig == null) {
            // Currently we only start dev-services for the default backend of the default persistence unit.
            // See https://github.com/quarkusio/quarkus/issues/24011
            return null;
        }
        var defaultPUDefaultBackendConfig = defaultPUConfig.backends().get(null);
        if (defaultPUDefaultBackendConfig == null
                || !defaultPUDefaultBackendConfig.version().isPresent()) {
            // If the version is not set, the default backend is not in use.
            return null;
        }
        Optional<Boolean> active = ConfigUtils.getFirstOptionalValue(
                mapperPropertyKeys(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, "active"), Boolean.class);
        if (active.isPresent() && !active.get()) {
            // If Hibernate Search is deactivated, we don't want to trigger dev services.
            return null;
        }
        ElasticsearchVersion version = defaultPUDefaultBackendConfig.version().get();
        String hostsPropertyKey = backendPropertyKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, null, null,
                "hosts");
        return new DevservicesElasticsearchBuildItem(hostsPropertyKey,
                version.versionString(),
                Distribution.valueOf(version.distribution().toString().toUpperCase()));
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    void devServicesDropAndCreateAndDropByDefault(
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfigProducer) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            String puName = configuredPersistenceUnit.getPersistenceUnitName();
            List<String> propertyKeysIndicatingHostsConfigured = defaultBackendPropertyKeys(puName, "hosts");

            if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingHostsConfigured)) {
                String schemaManagementStrategyPropertyKey = mapperPropertyKey(puName, "schema-management.strategy");
                if (!ConfigUtils.isPropertyPresent(schemaManagementStrategyPropertyKey)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                // Only force DB generation if the datasource is configured through dev services
                                if (propertyKeysIndicatingHostsConfigured.stream()
                                        .anyMatch(devServicesConfig::containsKey)) {
                                    String forcedValue = "drop-and-create-and-drop";
                                    LOG.infof("Setting %s=%s to initialize Dev Services managed Elasticsearch server",
                                            schemaManagementStrategyPropertyKey, forcedValue);
                                    return Map.of(schemaManagementStrategyPropertyKey, forcedValue);
                                } else {
                                    return Map.of();
                                }
                            }));
                }
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = HibernateSearchManagementEnabled.class)
    void createManagementRoutes(BuildProducer<RouteBuildItem> routes,
            HibernateSearchElasticsearchRecorder recorder,
            HibernateSearchElasticsearchBuildTimeConfig hibernateSearchElasticsearchBuildTimeConfig) {

        String managementRootPath = hibernateSearchElasticsearchBuildTimeConfig.management().rootPath();

        routes.produce(RouteBuildItem.newManagementRoute(
                managementRootPath + (managementRootPath.endsWith("/") ? "" : "/") + "reindex")
                .withRoutePathConfigKey("quarkus.hibernate-search-orm.management.root-path")
                .withRequestHandler(recorder.managementHandler())
                .displayOnNotFoundPage()
                .build());
    }
}
