package io.quarkus.hibernate.search.backend.elasticsearch.common.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonClasses;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.MapperContext;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps
class HibernateSearchBackendElasticsearchProcessor {

    @BuildStep
    void registerBeans(List<HibernateSearchBackendElasticsearchEnabledBuildItem> enabled,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        if (enabled.isEmpty()) {
            return;
        }
        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(ElasticsearchAnalysisConfigurer.class,
                IndexLayoutStrategy.class));
    }

    @BuildStep
    void registerReflectionForGson(List<HibernateSearchBackendElasticsearchEnabledBuildItem> enabled,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (enabled.isEmpty()) {
            return;
        }
        String[] reflectiveClasses = GsonClasses.typesRequiringReflection().toArray(String[]::new);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(reflectiveClasses)
                .reason(getClass().getName())
                .methods().fields().build());
    }

    @BuildStep
    void processBuildTimeConfig(List<HibernateSearchBackendElasticsearchEnabledBuildItem> enabled,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        if (enabled.isEmpty()) {
            return;
        }
        for (HibernateSearchBackendElasticsearchEnabledBuildItem enabledItem : enabled) {
            processBuildTimeConfig(enabledItem, applicationArchivesBuildItem, nativeImageResources,
                    hotDeploymentWatchedFiles);
        }
    }

    private void processBuildTimeConfig(HibernateSearchBackendElasticsearchEnabledBuildItem enabled,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        Set<String> propertyKeysWithNoVersion = new LinkedHashSet<>();
        var buildTimeConfig = enabled.getBuildTimeConfig();

        var mapperContext = enabled.getMapperContext();
        Set<String> allBackendNames = new LinkedHashSet<>(mapperContext.getBackendNamesForIndexedEntities());
        allBackendNames.addAll(buildTimeConfig.keySet());
        // For all backends referenced either through @Indexed(backend = ...) or configuration...
        for (String backendName : allBackendNames) {
            HibernateSearchBackendElasticsearchBuildTimeConfig backendConfig = buildTimeConfig.get(backendName);
            // ... we validate that the backend is configured and the version is present
            if (backendConfig == null || backendConfig.version().isEmpty()) {
                propertyKeysWithNoVersion.add(mapperContext.backendPropertyKey(backendName, null, "version"));
            }
            if (backendConfig == null) {
                continue;
            }
            // ... we register files referenced from backends configuration
            registerClasspathFilesFromBackendConfig(mapperContext, backendName, backendConfig,
                    applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        }
        if (!propertyKeysWithNoVersion.isEmpty()) {
            throw new ConfigurationException(
                    "The Elasticsearch version needs to be defined via properties: "
                            + String.join(", ", propertyKeysWithNoVersion) + ".",
                    propertyKeysWithNoVersion);
        }
    }

    private static void registerClasspathFilesFromBackendConfig(MapperContext mapperContext, String backendName,
            HibernateSearchBackendElasticsearchBuildTimeConfig backendConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        registerClasspathFilesFromIndexConfig(mapperContext, backendName, null, backendConfig.indexDefaults(),
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        for (Entry<String, HibernateSearchBackendElasticsearchBuildTimeConfig.IndexConfig> entry : backendConfig.indexes()
                .entrySet()) {
            String indexName = entry.getKey();
            HibernateSearchBackendElasticsearchBuildTimeConfig.IndexConfig indexConfig = entry.getValue();
            registerClasspathFilesFromIndexConfig(mapperContext, backendName, indexName, indexConfig,
                    applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        }
    }

    private static void registerClasspathFilesFromIndexConfig(MapperContext mapperContext, String backendName, String indexName,
            HibernateSearchBackendElasticsearchBuildTimeConfig.IndexConfig indexConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        registerClasspathFileFromConfig(mapperContext, backendName, indexName, "schema-management.settings-file",
                indexConfig.schemaManagement().settingsFile(),
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
        registerClasspathFileFromConfig(mapperContext, backendName, indexName, "schema-management.mapping-file",
                indexConfig.schemaManagement().mappingFile(),
                applicationArchivesBuildItem, nativeImageResources, hotDeploymentWatchedFiles);
    }

    private static void registerClasspathFileFromConfig(MapperContext mapperContext, String backendName, String indexName,
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
                            + mapperContext.backendPropertyKey(backendName, indexName, propertyKeyRadical) + "="
                            + classpathFile
                            + "'. Remove property or add file to your path.");
        }
        nativeImageResources.produce(new NativeImageResourceBuildItem(classpathFile));
        hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(classpathFile));
    }

}
