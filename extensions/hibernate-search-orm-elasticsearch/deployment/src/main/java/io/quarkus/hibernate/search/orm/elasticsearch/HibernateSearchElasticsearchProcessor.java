package io.quarkus.hibernate.search.orm.elasticsearch;

import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.GSON_CLASSES;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.INDEXED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.ElasticsearchVersionSubstitution;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;

class HibernateSearchElasticsearchProcessor {

    private static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search ORM + Elasticsearch";

    HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchIntegrator", "HSEARCH000034"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(RecorderContext recorderContext, HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrations,
            BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.HIBERNATE_SEARCH_ELASTICSEARCH));

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);

        // Make it possible to record the ElasticsearchVersion as bytecode:
        recorderContext.registerSubstitution(ElasticsearchVersion.class,
                String.class, ElasticsearchVersionSubstitution.class);

        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            Collection<AnnotationInstance> indexedAnnotationsForPU = new ArrayList<>();
            for (AnnotationInstance indexedAnnotation : indexedAnnotations) {
                String targetName = indexedAnnotation.target().asClass().name().toString();
                if (puDescriptor.getManagedClassNames().contains(targetName)) {
                    indexedAnnotationsForPU.add(indexedAnnotation);
                }
            }
            buildForPersistenceUnit(recorder, indexedAnnotationsForPU, puDescriptor.getPersistenceUnitName(),
                    configuredPersistenceUnits, integrations);
        }

        registerReflectionForGson(reflectiveClass);
    }

    private void buildForPersistenceUnit(HibernateSearchElasticsearchRecorder recorder,
            Collection<AnnotationInstance> indexedAnnotationsForPU, String persistenceUnitName,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrations) {
        if (indexedAnnotationsForPU.isEmpty()) {
            // we don't have any indexed entity, we can disable Hibernate Search
            integrations.produce(new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    persistenceUnitName).setInitListener(recorder.createDisabledListener()));
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

        checkConfig(persistenceUnitName, puConfig, defaultBackendIsUsed);

        configuredPersistenceUnits
                .produce(new HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem(persistenceUnitName));

        if (puConfig == null) {
            return;
        }

        integrations.produce(new HibernateOrmIntegrationStaticConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                persistenceUnitName).setInitListener(recorder.createStaticInitListener(puConfig)));
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
                    integrationRuntimeInitListeners.add(item.getInitListener());
                }
            }
            runtimeConfigured.produce(
                    new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH, puName)
                            .setInitListener(
                                    recorder.createRuntimeInitListener(runtimeConfig, puName,
                                            integrationRuntimeInitListeners)));
        }
    }

    private static void checkConfig(String persistenceUnitName,
            HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit buildTimeConfig, boolean defaultBackendIsUsed) {
        List<String> propertyKeysWithNoVersion = new ArrayList<>();
        if (defaultBackendIsUsed) {
            // we validate that the version is present for the default backend
            if (buildTimeConfig == null || !buildTimeConfig.defaultBackend.version.isPresent()) {
                propertyKeysWithNoVersion.add(elasticsearchVersionPropertyKey(persistenceUnitName, null));
            }
        }

        // we validate that the version is present for all the named backends
        Map<String, ElasticsearchBackendBuildTimeConfig> backends = buildTimeConfig != null
                ? buildTimeConfig.namedBackends.backends
                : Collections.emptyMap();
        for (Entry<String, ElasticsearchBackendBuildTimeConfig> additionalBackendEntry : backends.entrySet()) {
            if (!additionalBackendEntry.getValue().version.isPresent()) {
                propertyKeysWithNoVersion
                        .add(elasticsearchVersionPropertyKey(persistenceUnitName, additionalBackendEntry.getKey()));
            }
        }
        if (!propertyKeysWithNoVersion.isEmpty()) {
            throw new ConfigurationError(
                    "The Elasticsearch version needs to be defined via properties: "
                            + String.join(", ", propertyKeysWithNoVersion) + ".");
        }
    }

    private static String elasticsearchVersionPropertyKey(String persistenceUnitName, String backendName) {
        StringBuilder keyBuilder = new StringBuilder("quarkus.hibernate-search-orm.");
        if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            keyBuilder.append(persistenceUnitName).append(".");
        }
        keyBuilder.append("elasticsearch.");
        if (backendName != null) {
            keyBuilder.append(backendName).append(".");
        }
        keyBuilder.append("version");
        return keyBuilder.toString();
    }

    private void registerReflectionForGson(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String[] reflectiveClasses = GSON_CLASSES.stream().map(DotName::toString).toArray(String[]::new);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, reflectiveClasses));
    }
}
