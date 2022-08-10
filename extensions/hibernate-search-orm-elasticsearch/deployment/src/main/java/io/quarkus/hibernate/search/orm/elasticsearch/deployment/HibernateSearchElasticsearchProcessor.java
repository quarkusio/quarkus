package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import static io.quarkus.hibernate.search.orm.elasticsearch.deployment.ClassNames.INDEXED;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.backendPropertyKey;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.defaultBackendPropertyKeys;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.elasticsearchVersionPropertyKey;
import static io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.mapperPropertyKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonClasses;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.ElasticsearchVersionSubstitution;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchIndexBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = HibernateSearchEnabled.class)
class HibernateSearchElasticsearchProcessor {

    static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search ORM + Elasticsearch";

    private static final Logger LOG = Logger.getLogger(HibernateSearchElasticsearchProcessor.class);

    HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);

        Map<String, Map<String, Set<String>>> persistenceUnitAndBackendAndIndexNamesForSearchExtensions = collectPersistenceUnitAndBackendAndIndexNamesForSearchExtensions(
                index);

        Map<String, HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit> configByPU = buildTimeConfig
                .getAllPersistenceUnitConfigsAsMap();

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

        registerReflectionForGson(reflectiveClass);
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
                .produce(new HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(persistenceUnitName, puConfig,
                        backendNamesForIndexedEntities, backendAndIndexNamesForSearchExtensions));
    }

    @BuildStep
    void registerBeans(List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> searchEnabledPUs,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        if (searchEnabledPUs.isEmpty()) {
            return;
        }

        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(FailureHandler.class,
                AutomaticIndexingSynchronizationStrategy.class,
                ElasticsearchAnalysisConfigurer.class, IndexLayoutStrategy.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setStaticConfig(RecorderContext recorderContext, HibernateSearchElasticsearchRecorder recorder,
            List<HibernateSearchIntegrationStaticConfiguredBuildItem> integrationStaticConfigBuildItems,
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticConfigured) {
        // Make it possible to record the settings as bytecode:
        recorderContext.registerSubstitution(ElasticsearchVersion.class,
                String.class, ElasticsearchVersionSubstitution.class);

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
                                    recorder.createStaticInitListener(puName, configuredPersistenceUnit.getBuildTimeConfig(),
                                            configuredPersistenceUnit.getBackendAndIndexNamesForSearchExtensions(),
                                            integrationStaticInitListeners))
                            .setXmlMappingRequired(xmlMappingRequired));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchElasticsearchRecorder recorder,
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
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
                            .setInitListener(
                                    recorder.createRuntimeInitListener(runtimeConfig, puName,
                                            configuredPersistenceUnit.getBackendAndIndexNamesForSearchExtensions(),
                                            integrationRuntimeInitListeners)));
        }
    }

    @BuildStep
    public void processPersistenceUnitBuildTimeConfig(
            List<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        for (HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit : configuredPersistenceUnits) {
            processPersistenceUnitBuildTimeConfig(configuredPersistenceUnit, applicationArchivesBuildItem, nativeImageResources,
                    hotDeploymentWatchedFiles);
        }
    }

    private void processPersistenceUnitBuildTimeConfig(
            HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem configuredPersistenceUnit,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        String persistenceUnitName = configuredPersistenceUnit.getPersistenceUnitName();
        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig = configuredPersistenceUnit
                .getBuildTimeConfig();

        Set<String> propertyKeysWithNoVersion = new LinkedHashSet<>();
        Map<String, ElasticsearchBackendBuildTimeConfig> backends = buildTimeConfig != null
                ? buildTimeConfig.getAllBackendConfigsAsMap()
                : Collections.emptyMap();

        Set<String> allBackendNames = new LinkedHashSet<>(configuredPersistenceUnit.getBackendNamesForIndexedEntities());
        allBackendNames.addAll(backends.keySet());
        // For all backends referenced either through @Indexed(backend = ...) or configuration...
        for (String backendName : allBackendNames) {
            ElasticsearchBackendBuildTimeConfig backendConfig = backends.get(backendName);
            // ... we validate that the backend is configured and the version is present
            if (backendConfig == null || backendConfig.version.isEmpty()) {
                propertyKeysWithNoVersion.add(elasticsearchVersionPropertyKey(persistenceUnitName, backendName));
            }
            // ... we register files referenced from backends configuration
            if (backendConfig != null) {
                registerClasspathFileFromBackendConfig(persistenceUnitName, backendName, backendConfig,
                        applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
            }
        }
        if (!propertyKeysWithNoVersion.isEmpty()) {
            throw new ConfigurationException(
                    "The Elasticsearch version needs to be defined via properties: "
                            + String.join(", ", propertyKeysWithNoVersion) + ".",
                    propertyKeysWithNoVersion);
        }
    }

    private static void registerClasspathFileFromBackendConfig(String persistenceUnitName, String backendName,
            ElasticsearchBackendBuildTimeConfig backendConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        registerClasspathFileFromIndexConfig(persistenceUnitName, backendName, null, backendConfig.indexDefaults,
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        for (Entry<String, ElasticsearchIndexBuildTimeConfig> entry : backendConfig.indexes.entrySet()) {
            String indexName = entry.getKey();
            ElasticsearchIndexBuildTimeConfig indexConfig = entry.getValue();
            registerClasspathFileFromIndexConfig(persistenceUnitName, backendName, indexName, indexConfig,
                    applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        }
    }

    private static void registerClasspathFileFromIndexConfig(String persistenceUnitName, String backendName, String indexName,
            ElasticsearchIndexBuildTimeConfig indexConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        registerClasspathFileFromConfig(persistenceUnitName, backendName, indexName, "schema-management.settings-file",
                indexConfig.schemaManagement.settingsFile,
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        registerClasspathFileFromConfig(persistenceUnitName, backendName, indexName, "schema-management.mapping-file",
                indexConfig.schemaManagement.mappingFile,
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
    }

    private static void registerClasspathFileFromConfig(String persistenceUnitName, String backendName, String indexName,
            String propertyKeyRadical,
            Optional<String> classpathFileOptional,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        if (!classpathFileOptional.isPresent()) {
            return;
        }
        String classpathFile = classpathFileOptional.get();

        Path existingPath = applicationArchivesBuildItem.getRootArchive().getChildPath(classpathFile);

        if (existingPath == null || Files.isDirectory(existingPath)) {
            //raise exception if explicit file is not present (i.e. not the default)
            throw new ConfigurationException(
                    "Unable to find file referenced in '"
                            + backendPropertyKey(persistenceUnitName, backendName, indexName, propertyKeyRadical) + "="
                            + classpathFile
                            + "'. Remove property or add file to your path.");
        }
        nativeImageResources.produce(new NativeImageResourceBuildItem(classpathFile));
        hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(classpathFile));
    }

    private void registerReflectionForGson(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] reflectiveClasses = GsonClasses.typesRequiringReflection().toArray(String[]::new);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, reflectiveClasses));
    }

    @BuildStep
    DevservicesElasticsearchBuildItem devServices(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
        if (buildTimeConfig.defaultPersistenceUnit != null && buildTimeConfig.defaultPersistenceUnit.defaultBackend != null
        // If the version is not set, the default backend is not in use.
                && buildTimeConfig.defaultPersistenceUnit.defaultBackend.version.isPresent()) {
            ElasticsearchVersion version = buildTimeConfig.defaultPersistenceUnit.defaultBackend.version.get();
            String hostsPropertyKey = backendPropertyKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, null, null,
                    "hosts");
            return new DevservicesElasticsearchBuildItem(hostsPropertyKey,
                    version.versionString(),
                    DevservicesElasticsearchBuildItem.Distribution.valueOf(version.distribution().toString().toUpperCase()));
        } else {
            // Currently we only start dev-services for the default backend of the default persistence unit.
            // See https://github.com/quarkusio/quarkus/issues/24011
            return null;
        }
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
                    String forcedValue = "drop-and-create-and-drop";
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(propertyKeysIndicatingHostsConfigured,
                                    schemaManagementStrategyPropertyKey, forcedValue,
                                    () -> LOG.infof("Setting %s=%s to initialize Dev Services managed Elasticsearch server",
                                            schemaManagementStrategyPropertyKey, forcedValue)));
                }
            }
        }
    }

}
