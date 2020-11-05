package io.quarkus.hibernate.search.orm.elasticsearch;

import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.GSON_CLASSES;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.INDEXED;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.PROPERTY_MAPPING_META_ANNOTATION;
import static io.quarkus.hibernate.search.orm.elasticsearch.HibernateSearchClasses.TYPE_MAPPING_META_ANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;

class HibernateSearchElasticsearchProcessor {

    private static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search Elasticsearch";

    HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchIntegrator", "HSEARCH000034"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateOrmIntegrationBuildItem> integrations,
            BuildProducer<FeatureBuildItem> feature) throws Exception {
        feature.produce(new FeatureBuildItem(Feature.HIBERNATE_SEARCH_ELASTICSEARCH));

        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<AnnotationInstance> indexedAnnotations = index.getAnnotations(INDEXED);
        if (indexedAnnotations.isEmpty()) {
            // we don't have any indexed entity, we can bail out
            return;
        }

        boolean defaultBackendIsUsed = false;
        for (AnnotationInstance indexedAnnotation : indexedAnnotations) {
            if (indexedAnnotation.value("backend") == null) {
                defaultBackendIsUsed = true;
                break;
            }
        }

        checkConfig(buildTimeConfig, defaultBackendIsUsed);

        // Register the Hibernate Search integration
        integrations.produce(new HibernateOrmIntegrationBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH));

        // Register the required reflection declarations
        registerReflection(index, reflectiveClass);

        // Register the Hibernate Search integration listener
        recorder.registerHibernateSearchIntegration(buildTimeConfig);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setRuntimeConfig(HibernateSearchElasticsearchRecorder recorder,
            HibernateSearchElasticsearchRuntimeConfig runtimeConfig,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        recorder.setRuntimeConfig(runtimeConfig);

        runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH));
    }

    private static void checkConfig(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig,
            boolean defaultBackendIsUsed) {
        if (defaultBackendIsUsed) {
            // we validate that the version is present for the default backend
            if (!buildTimeConfig.defaultBackend.version.isPresent()) {
                throw new ConfigurationError(
                        "The Elasticsearch version needs to be defined via the quarkus.hibernate-search-orm.elasticsearch.version property.");
            }
        }

        // we validate that the version is present for all the named backends
        List<String> namedBackendsWithNoVersion = new ArrayList<>();
        for (Entry<String, ElasticsearchBackendBuildTimeConfig> additionalBackendEntry : buildTimeConfig.namedBackends.backends
                .entrySet()) {
            if (!additionalBackendEntry.getValue().version.isPresent()) {
                namedBackendsWithNoVersion.add(additionalBackendEntry.getKey());
            }
        }
        if (!namedBackendsWithNoVersion.isEmpty()) {
            throw new ConfigurationError("The Elasticsearch version property needs to be defined for backends "
                    + String.join(", ", namedBackendsWithNoVersion));
        }
    }

    private void registerReflection(IndexView index, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<DotName> reflectiveClassCollector = new HashSet<>();

        if (buildTimeConfig.defaultBackend.indexDefaults.analysis.configurer.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false,
                            buildTimeConfig.defaultBackend.indexDefaults.analysis.configurer.get()));
        }
        for (HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchIndexBuildTimeConfig indexConfig : buildTimeConfig.defaultBackend.indexes
                .values()) {
            if (indexConfig.analysis.configurer.isPresent()) {
                reflectiveClass.produce(
                        new ReflectiveClassBuildItem(true, false, indexConfig.analysis.configurer.get()));
            }
        }

        if (buildTimeConfig.defaultBackend.layout.strategy.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, buildTimeConfig.defaultBackend.layout.strategy.get()));
        }

        if (buildTimeConfig.backgroundFailureHandler.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, buildTimeConfig.backgroundFailureHandler.get()));
        }

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
