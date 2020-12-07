package io.quarkus.hibernate.search.orm.elasticsearch;

import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.GSON_CLASSES;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.INDEXED;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.PROPERTY_MAPPING_META_ANNOTATION;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.TYPE_MAPPING_META_ANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.VoidType;

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
            buildForPersistenceUnit(recorder, indexedAnnotationsForPU, puDescriptor.getPersistenceUnitName(), reflectiveClass,
                    configuredPersistenceUnits, integrations);
        }

        registerReflectionForClasses(index, reflectiveClass);
    }

    private void buildForPersistenceUnit(HibernateSearchElasticsearchRecorder recorder,
            Collection<AnnotationInstance> indexedAnnotationsForPU, String persistenceUnitName,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateSearchElasticsearchPersistenceUnitConfiguredBuildItem> configuredPersistenceUnits,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrations) {
        if (indexedAnnotationsForPU.isEmpty()) {
            // we don't have any indexed entity, we can bail out
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
                persistenceUnitName, recorder.createStaticInitListener(puConfig)));

        registerReflectionForConfig(puConfig, reflectiveClass);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchElasticsearchRecorder recorder,
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH,
                    puDescriptor.getPersistenceUnitName(),
                    recorder.createRuntimeInitListener(runtimeConfig, puDescriptor.getPersistenceUnitName())));
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

    private void registerReflectionForConfig(HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit puConfig,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (puConfig.defaultBackend.indexDefaults.analysis.configurer.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false,
                            puConfig.defaultBackend.indexDefaults.analysis.configurer.get()));
        }
        for (HibernateSearchElasticsearchBuildTimeConfigPersistenceUnit.ElasticsearchIndexBuildTimeConfig indexConfig : puConfig.defaultBackend.indexes
                .values()) {
            if (indexConfig.analysis.configurer.isPresent()) {
                reflectiveClass.produce(
                        new ReflectiveClassBuildItem(true, false, indexConfig.analysis.configurer.get()));
            }
        }

        if (puConfig.defaultBackend.layout.strategy.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, puConfig.defaultBackend.layout.strategy.get()));
        }

        if (puConfig.backgroundFailureHandler.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, puConfig.backgroundFailureHandler.get()));
        }
    }

    private void registerReflectionForClasses(IndexView index, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<DotName> reflectiveClassCollector = new HashSet<>();

        for (AnnotationInstance propertyMappingMetaAnnotationInstance : index
                .getAnnotations(PROPERTY_MAPPING_META_ANNOTATION)) {
            for (AnnotationInstance propertyMappingAnnotationInstance : index
                    .getAnnotations(propertyMappingMetaAnnotationInstance.name())) {
                AnnotationTarget annotationTarget = propertyMappingAnnotationInstance.target();
                if (annotationTarget.kind() == Kind.FIELD) {
                    FieldInfo fieldInfo = annotationTarget.asField();
                    addReflectiveClass(index, reflectiveClassCollector, fieldInfo.declaringClass());
                    addReflectiveType(index, reflectiveClassCollector, fieldInfo.type());
                } else if (annotationTarget.kind() == Kind.METHOD) {
                    MethodInfo methodInfo = annotationTarget.asMethod();
                    addReflectiveClass(index, reflectiveClassCollector, methodInfo.declaringClass());
                    addReflectiveType(index, reflectiveClassCollector, methodInfo.returnType());
                }
            }
        }

        for (AnnotationInstance typeBridgeMappingInstance : index.getAnnotations(TYPE_MAPPING_META_ANNOTATION)) {
            for (AnnotationInstance typeBridgeInstance : index.getAnnotations(typeBridgeMappingInstance.name())) {
                addReflectiveClass(index, reflectiveClassCollector, typeBridgeInstance.target().asClass());
            }
        }

        reflectiveClassCollector.addAll(GSON_CLASSES);

        String[] reflectiveClasses = reflectiveClassCollector.stream().map(DotName::toString).toArray(String[]::new);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, reflectiveClasses));
    }

    private static void addReflectiveClass(IndexView index, Set<DotName> reflectiveClassCollector,
            ClassInfo classInfo) {
        if (skipClass(classInfo.name(), reflectiveClassCollector)) {
            return;
        }

        reflectiveClassCollector.add(classInfo.name());

        for (ClassInfo subclass : index.getAllKnownSubclasses(classInfo.name())) {
            reflectiveClassCollector.add(subclass.name());
        }
        for (ClassInfo implementor : index.getAllKnownImplementors(classInfo.name())) {
            reflectiveClassCollector.add(implementor.name());
        }

        Type superClassType = classInfo.superClassType();
        while (superClassType != null && !superClassType.name().toString().equals("java.lang.Object")) {
            reflectiveClassCollector.add(superClassType.name());
            if (superClassType instanceof ClassType) {
                superClassType = index.getClassByName(superClassType.name()).superClassType();
            } else if (superClassType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = superClassType.asParameterizedType();
                for (Type typeArgument : parameterizedType.arguments()) {
                    addReflectiveType(index, reflectiveClassCollector, typeArgument);
                }
                superClassType = parameterizedType.owner();
            }
        }
    }

    private static void addReflectiveType(IndexView index, Set<DotName> reflectiveClassCollector, Type type) {
        if (type instanceof VoidType || type instanceof PrimitiveType || type instanceof UnresolvedTypeVariable) {
            return;
        } else if (type instanceof ClassType) {
            ClassInfo classInfo = index.getClassByName(type.name());
            addReflectiveClass(index, reflectiveClassCollector, classInfo);
        } else if (type instanceof ArrayType) {
            addReflectiveType(index, reflectiveClassCollector, type.asArrayType().component());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = type.asParameterizedType();
            addReflectiveType(index, reflectiveClassCollector, parameterizedType.owner());
            for (Type typeArgument : parameterizedType.arguments()) {
                addReflectiveType(index, reflectiveClassCollector, typeArgument);
            }
        }
    }

    private static boolean skipClass(DotName name, Set<DotName> processedClasses) {
        return name.toString().startsWith("java.") || processedClasses.contains(name);
    }
}
