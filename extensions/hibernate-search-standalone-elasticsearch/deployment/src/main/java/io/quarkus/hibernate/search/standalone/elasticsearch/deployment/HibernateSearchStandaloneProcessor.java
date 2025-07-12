package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import static io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchTypes.BUILT_IN_ROOT_MAPPING_ANNOTATIONS;
import static io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchTypes.INDEXED;
import static io.quarkus.hibernate.search.standalone.elasticsearch.deployment.HibernateSearchTypes.ROOT_MAPPING;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.backendPropertyKey;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.defaultBackendPropertyKeys;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.mapperPropertyKey;
import static io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRuntimeConfig.mapperPropertyKeys;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.elasticsearch.restclient.common.deployment.DevservicesElasticsearchBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;
import io.quarkus.hibernate.search.backend.elasticsearch.common.deployment.HibernateSearchBackendElasticsearchEnabledBuildItem;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.ElasticsearchVersionSubstitution;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneBuildTimeConfig;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneElasticsearchMapperContext;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRecorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

@BuildSteps(onlyIf = HibernateSearchStandaloneEnabled.class)
class HibernateSearchStandaloneProcessor {

    private static final Logger LOG = Logger.getLogger(HibernateSearchStandaloneProcessor.class);

    @BuildStep
    void registerAnnotations(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @SearchExtension class
        // otherwise it won't be registered as qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(HibernateSearchTypes.SEARCH_EXTENSION.toString())
                .build());

        // Register the default scope for @SearchExtension and make such beans unremovable by default
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(HibernateSearchTypes.SEARCH_EXTENSION, DotNames.APPLICATION_SCOPED,
                        false));
    }

    @BuildStep
    public void configure(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<HibernateSearchStandaloneEnabledBuildItem> enabled) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);
        if (indexedAnnotations.isEmpty()) {
            // we don't have any indexed entity, we can disable Hibernate Search
            return;
        }

        Set<String> backendNamesForIndexedEntities = new LinkedHashSet<>();
        for (AnnotationInstance indexedAnnotation : indexedAnnotations) {
            AnnotationValue backendNameValue = indexedAnnotation.value("backend");
            String backendName = backendNameValue == null ? null : backendNameValue.asString();
            backendNamesForIndexedEntities.add(backendName);
        }

        Map<String, Set<String>> backendAndIndexNamesForSearchExtensions = collectBackendAndIndexNamesForSearchExtensions(
                index);

        Set<String> rootAnnotationMappedClassNames = collectRootAnnotationMappedClassNames(index);

        var mapperContext = new HibernateSearchStandaloneElasticsearchMapperContext(backendNamesForIndexedEntities,
                backendAndIndexNamesForSearchExtensions);
        enabled.produce(new HibernateSearchStandaloneEnabledBuildItem(mapperContext, rootAnnotationMappedClassNames));
    }

    @BuildStep
    void enableBackend(Optional<HibernateSearchStandaloneEnabledBuildItem> enabled,
            HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
            BuildProducer<HibernateSearchBackendElasticsearchEnabledBuildItem> elasticsearchEnabled) {
        if (!enabled.isPresent()) {
            // No boot
            return;
        }
        elasticsearchEnabled.produce(new HibernateSearchBackendElasticsearchEnabledBuildItem(enabled.get().mapperContext,
                buildTimeConfig.backends()));
    }

    private static Map<String, Set<String>> collectBackendAndIndexNamesForSearchExtensions(
            IndexView index) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (AnnotationInstance annotation : index.getAnnotations(HibernateSearchTypes.SEARCH_EXTENSION)) {
            var backendName = annotation.value("backend");
            var indexName = annotation.value("index");
            Set<String> indexNames = result
                    .computeIfAbsent(backendName == null ? null : backendName.asString(), ignored -> new LinkedHashSet<>());
            if (indexName != null) {
                indexNames.add(indexName.asString());
            }
        }
        return result;
    }

    private static Set<String> collectRootAnnotationMappedClassNames(IndexView index) {
        // Look for classes annotated with annotations meta-annotated with @RootMapping:
        // those classes will have their annotations processed.
        // Built-in annotations from Hibernate Search must be added explicitly,
        // because Hibernate Search may not be part of the index.
        Set<DotName> rootMappingAnnotationNames = new LinkedHashSet<>(BUILT_IN_ROOT_MAPPING_ANNOTATIONS);
        // We'll also consider @Indexed as a "root mapping" annotation,
        // even if that's not true in Hibernate Search,
        // because it's more convenient with the Standalone mapper.
        rootMappingAnnotationNames.add(INDEXED);
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

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void defineSearchMappingBean(Optional<HibernateSearchStandaloneEnabledBuildItem> enabled,
            HibernateSearchStandaloneRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (!enabled.isPresent()) {
            // No boot
            return;
        }
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(SearchMapping.class)
                // NOTE: this is using ApplicationScoped and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .addQualifier(Default.class)
                .setRuntimeInit()
                .createWith(recorder.createSearchMappingFunction(enabled.get().mapperContext))
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @Consume(BeanContainerBuildItem.class) // Pre-boot needs access to the CDI container
    public void preBoot(Optional<HibernateSearchStandaloneEnabledBuildItem> enabled,
            RecorderContext recorderContext,
            HibernateSearchStandaloneRecorder recorder) {
        if (enabled.isEmpty()) {
            // No pre-boot
            return;
        }

        // Make it possible to record the settings as bytecode:
        recorderContext.registerSubstitution(ElasticsearchVersion.class,
                String.class, ElasticsearchVersionSubstitution.class);
        recorder.preBoot(enabled.get().mapperContext, enabled.get().getRootAnnotationMappedClassNames());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BeanContainerBuildItem.class)
    void boot(Optional<HibernateSearchStandaloneEnabledBuildItem> enabled,
            HibernateSearchStandaloneRecorder recorder,
            BuildProducer<ServiceStartBuildItem> serviceStart) {
        if (enabled.isEmpty()) {
            // No boot
            return;
        }
        recorder.bootEagerly();
        serviceStart.produce(new ServiceStartBuildItem("Hibernate Search Standalone"));
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    void devServices(Optional<HibernateSearchStandaloneEnabledBuildItem> enabled,
            HibernateSearchStandaloneBuildTimeConfig buildTimeConfig,
            BuildProducer<DevservicesElasticsearchBuildItem> buildItemBuildProducer,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfigProducer) {
        if (enabled.isEmpty()) {
            // No dev services necessary
            return;
        }

        // Currently we only start dev-services for the default backend
        // See https://github.com/quarkusio/quarkus/issues/24011
        var defaultBackendConfig = buildTimeConfig.backends().get(null);
        if (defaultBackendConfig == null || !defaultBackendConfig.version().isPresent()) {
            // If the version is not set, the default backend is not in use.
            return;
        }
        Optional<Boolean> active = ConfigUtils.getFirstOptionalValue(
                mapperPropertyKeys("active"), Boolean.class);
        if (active.isPresent() && !active.get()) {
            // If Hibernate Search is deactivated, we don't want to trigger dev services.
            return;
        }
        ElasticsearchVersion version = defaultBackendConfig.version().get();
        String hostsPropertyKey = backendPropertyKey(null, null,
                "hosts");
        buildItemBuildProducer.produce(new DevservicesElasticsearchBuildItem(hostsPropertyKey,
                version.versionString(),
                Distribution.valueOf(version.distribution().toString().toUpperCase())));

        // Force schema generation when using dev services
        List<String> propertyKeysIndicatingHostsConfigured = defaultBackendPropertyKeys("hosts");
        if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingHostsConfigured)) {
            String schemaManagementStrategyPropertyKey = mapperPropertyKey("schema-management.strategy");
            if (!ConfigUtils.isPropertyPresent(schemaManagementStrategyPropertyKey)) {
                devServicesAdditionalConfigProducer
                        .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
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

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = HibernateSearchStandaloneManagementEnabled.class)
    void createManagementRoutes(BuildProducer<RouteBuildItem> routes,
            HibernateSearchStandaloneRecorder recorder,
            HibernateSearchStandaloneBuildTimeConfig hibernateSearchStandaloneBuildTimeConfig) {

        String managementRootPath = hibernateSearchStandaloneBuildTimeConfig.management().rootPath();

        routes.produce(RouteBuildItem.newManagementRoute(
                managementRootPath + (managementRootPath.endsWith("/") ? "" : "/") + "reindex")
                .withRoutePathConfigKey("quarkus.hibernate-search-standalone.management.root-path")
                .withRequestHandler(recorder.managementHandler())
                .displayOnNotFoundPage()
                .build());
    }
}
