package io.quarkus.hibernate.search.orm.elasticsearch;

import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.INDEXED;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
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
import io.quarkus.runtime.configuration.ConfigurationException;

class HibernateSearchElasticsearchProcessor {

    private static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search ORM + Elasticsearch";

    HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        // if the category changes, please also update DisableLoggingAutoFeature in the runtime module
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchPreIntegrationService", "HSEARCH000034"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        feature.produce(new FeatureBuildItem(Feature.HIBERNATE_SEARCH_ELASTICSEARCH));

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);

        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            Collection<AnnotationInstance> indexedAnnotationsForPU = new ArrayList<>();
            for (AnnotationInstance indexedAnnotation : indexedAnnotations) {
                String targetName = indexedAnnotation.target().asClass().name().toString();
                if (puDescriptor.getManagedClassNames().contains(targetName)) {
                    indexedAnnotationsForPU.add(indexedAnnotation);
                }
            }
            buildForPersistenceUnit(recorder, indexedAnnotationsForPU, puDescriptor.getPersistenceUnitName(),
                    configuredPersistenceUnits, staticIntegrations, runtimeIntegrations);
        }

        registerReflectionForGson(reflectiveClass);
    }

    private void buildForPersistenceUnit(HibernateSearchElasticsearchRecorder recorder,
            Collection<AnnotationInstance> indexedAnnotationsForPU, String persistenceUnitName,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> staticIntegrations,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeIntegrations) {
        if (indexedAnnotationsForPU.isEmpty()) {
            // we don't have any indexed entity, we can disable Hibernate Search
            staticIntegrations.produce(new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    persistenceUnitName).setInitListener(recorder.createDisabledStaticInitListener()));
            // we need a runtime listener even when Hibernate Search is disabled,
            // just to let Hibernate Search boot up until the point where it checks whether it's enabled or not
            runtimeIntegrations.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    persistenceUnitName).setInitListener(recorder.createDisabledRuntimeInitListener()));
            return;
        }

        HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit puConfig = PersistenceUnitUtil
                .isDefaultPersistenceUnit(persistenceUnitName)
                        ? buildTimeConfig.defaultPersistenceUnit
                        : buildTimeConfig.persistenceUnits.get(persistenceUnitName);

        boolean defaultBackendIsUsed = false;
        for (AnnotationInstance indexedAnnotation : indexedAnnotationsForPU) {
            if (indexedAnnotation.value("backend") == null) {
                defaultBackendIsUsed = true;
                break;
            }
        }

        configuredPersistenceUnits
                .produce(new HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(persistenceUnitName, puConfig,
                        defaultBackendIsUsed));
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
        // Make it possible to record the ElasticsearchVersion as bytecode:
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
                            .setInitListener(recorder.createStaticInitListener(configuredPersistenceUnit.getBuildTimeConfig(),
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
        if (configuredPersistenceUnit.isDefaultBackendUsed()) {
            ElasticsearchBackendBuildTimeConfig backendConfig = buildTimeConfig != null ? buildTimeConfig.defaultBackend : null;
            // we validate that the version is present for the default backend
            if (backendConfig == null || !backendConfig.version.isPresent()) {
                propertyKeysWithNoVersion.add(elasticsearchVersionPropertyKey(persistenceUnitName, null));
            }
            if (backendConfig != null) {
                // we register files referenced from the default backend configuration
                registerClasspathFileFromBackendConfig(persistenceUnitName, null, backendConfig,
                        applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
            }
        }

        // we validate that the version is present for all the named backends
        Map<String, ElasticsearchBackendBuildTimeConfig> backends = buildTimeConfig != null
                ? buildTimeConfig.namedBackends.backends
                : Collections.emptyMap();
        for (Entry<String, ElasticsearchBackendBuildTimeConfig> additionalBackendEntry : backends.entrySet()) {
            String backendName = additionalBackendEntry.getKey();
            ElasticsearchBackendBuildTimeConfig backendConfig = additionalBackendEntry.getValue();
            if (!backendConfig.version.isPresent()) {
                propertyKeysWithNoVersion
                        .add(elasticsearchVersionPropertyKey(persistenceUnitName, backendName));
            }
            // we register files referenced from named backends configuration
            registerClasspathFileFromBackendConfig(persistenceUnitName, backendName, backendConfig,
                    applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
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

    private static String elasticsearchVersionPropertyKey(String persistenceUnitName, String backendName) {
        return backendPropertyKey(persistenceUnitName, backendName, null, "version");
    }

    private static String backendPropertyKey(String persistenceUnitName, String backendName, String indexName, String radical) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append(persistenceUnitName).append(".");
        }
        keyBuilder.append("elasticsearch.");
        if (backendName != null) {
            keyBuilder.append(backendName).append(".");
        }
        if (indexName != null) {
            keyBuilder.append("indexes.").append(indexName).append(".");
        }
        keyBuilder.append(radical);
        return keyBuilder.toString();
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
            return new DevservicesElasticsearchBuildItem("quarkus.hibernate-search-orm.elasticsearch.hosts",
                    version.versionString(),
                    DevservicesElasticsearchBuildItem.Distribution.valueOf(version.distribution().toString().toUpperCase()));
        } else {
            // Currently we only start dev-services for the default backend of the default persistence unit.
            // See https://github.com/quarkusio/quarkus/issues/24011
            return null;
        }
    }
}
