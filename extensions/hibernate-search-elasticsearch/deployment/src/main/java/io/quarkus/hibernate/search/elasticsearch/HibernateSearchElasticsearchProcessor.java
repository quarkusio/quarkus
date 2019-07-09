package io.quarkus.hibernate.search.elasticsearch;

import static io.quarkus.hibernate.search.elasticsearch.HibernateSearchClasses.FIELD_ANNOTATIONS;
import static io.quarkus.hibernate.search.elasticsearch.HibernateSearchClasses.INDEXED;
import static io.quarkus.hibernate.search.elasticsearch.HibernateSearchClasses.PROPERTY_BRIDGE_DECLARATION_ANNOTATION;
import static io.quarkus.hibernate.search.elasticsearch.HibernateSearchClasses.SCHEMA_MAPPING_CLASSES;
import static io.quarkus.hibernate.search.elasticsearch.HibernateSearchClasses.TYPE_BRIDGE_DECLARATION_ANNOTATION;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchRecorder;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;

class HibernateSearchElasticsearchProcessor {

    private static final String HIBERNATE_SEARCH_ELASTICSEARCH = "Hibernate Search Elasticsearch";

    HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void build(HibernateSearchElasticsearchRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<HibernateOrmIntegrationBuildItem> integrations,
            BuildProducer<FeatureBuildItem> feature) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.HIBERNATE_SEARCH_ELASTICSEARCH));

        IndexView index = combinedIndexBuildItem.getIndex();

        if (index.getAnnotations(INDEXED).isEmpty()) {
            // we don't have any indexed entity, we can bail out
            return;
        }

        checkConfig(buildTimeConfig);

        // Register the Hibernate Search integration
        integrations.produce(new HibernateOrmIntegrationBuildItem(HIBERNATE_SEARCH_ELASTICSEARCH));

        // Register the required reflection declarations
        registerReflection(index, reflectiveClass, reflectiveHierarchy);

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

    private static void checkConfig(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
        if (buildTimeConfig.defaultBackend.isPresent()) {
            // we have a default named backend
            if (buildTimeConfig.elasticsearch.version.isPresent()) {
                throw new ConfigurationError(
                        "quarkus.hibernate-search.elasticsearch.default-backend cannot be used in conjunction with a default backend configuration.");
            }
            if (!buildTimeConfig.additionalBackends.containsKey(buildTimeConfig.defaultBackend.get())) {
                throw new ConfigurationError(
                        "The default backend defined does not exist: " + buildTimeConfig.defaultBackend.get());
            }
        } else {
            // we are in the default backend case
            if (!buildTimeConfig.elasticsearch.version.isPresent()) {
                throw new ConfigurationError(
                        "The Elasticsearch version needs to be defined via the quarkus.hibernate-search.elasticsearch.version property.");
            }
        }

        // we validate that the version is present for all the additional backends
        if (!buildTimeConfig.additionalBackends.isEmpty()) {
            List<String> additionalBackendsWithNoVersion = new ArrayList<>();
            for (Entry<String, ElasticsearchBackendBuildTimeConfig> additionalBackendEntry : buildTimeConfig.additionalBackends
                    .entrySet()) {
                if (!additionalBackendEntry.getValue().version.isPresent()) {
                    additionalBackendsWithNoVersion.add(additionalBackendEntry.getKey());
                }
            }
            if (!additionalBackendsWithNoVersion.isEmpty()) {
                throw new ConfigurationError("The Elasticsearch version property needs to be defined for backends "
                        + String.join(", ", additionalBackendsWithNoVersion));
            }
        }
    }

    private void registerReflection(IndexView index, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
        Set<DotName> reflectiveClassCollector = new HashSet<>();
        Set<DotName> reflectiveTypeCollector = new HashSet<>();

        if (buildTimeConfig.elasticsearch.analysisConfigurer.isPresent()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, buildTimeConfig.elasticsearch.analysisConfigurer.get()));
        }

        for (DotName fieldAnnotation : FIELD_ANNOTATIONS) {
            for (AnnotationInstance fieldAnnotationInstance : index.getAnnotations(fieldAnnotation)) {
                AnnotationTarget annotationTarget = fieldAnnotationInstance.target();
                if (annotationTarget.kind() == Kind.FIELD) {
                    FieldInfo fieldInfo = annotationTarget.asField();
                    addReflectiveClass(index, reflectiveClassCollector, reflectiveTypeCollector, fieldInfo.declaringClass());
                    addReflectiveType(index, reflectiveTypeCollector, fieldInfo.type());
                } else if (annotationTarget.kind() == Kind.METHOD) {
                    MethodInfo methodInfo = annotationTarget.asMethod();
                    addReflectiveClass(index, reflectiveClassCollector, reflectiveTypeCollector, methodInfo.declaringClass());
                    addReflectiveType(index, reflectiveTypeCollector, methodInfo.returnType());
                }
            }
        }

        Set<Type> reflectiveHierarchyCollector = new HashSet<>();

        for (AnnotationInstance propertyBridgeMappingInstance : index.getAnnotations(PROPERTY_BRIDGE_DECLARATION_ANNOTATION)) {
            for (AnnotationInstance propertyBridgeInstance : index.getAnnotations(propertyBridgeMappingInstance.name())) {
                AnnotationTarget annotationTarget = propertyBridgeInstance.target();
                if (annotationTarget.kind() == Kind.FIELD) {
                    FieldInfo fieldInfo = annotationTarget.asField();
                    addReflectiveClass(index, reflectiveClassCollector, reflectiveTypeCollector, fieldInfo.declaringClass());
                    reflectiveHierarchyCollector.add(fieldInfo.type());
                } else if (annotationTarget.kind() == Kind.METHOD) {
                    MethodInfo methodInfo = annotationTarget.asMethod();
                    addReflectiveClass(index, reflectiveClassCollector, reflectiveTypeCollector, methodInfo.declaringClass());
                    reflectiveHierarchyCollector.add(methodInfo.returnType());
                }
            }
        }

        for (AnnotationInstance typeBridgeMappingInstance : index.getAnnotations(TYPE_BRIDGE_DECLARATION_ANNOTATION)) {
            for (AnnotationInstance typeBridgeInstance : index.getAnnotations(typeBridgeMappingInstance.name())) {
                addReflectiveClass(index, reflectiveClassCollector, reflectiveTypeCollector,
                        typeBridgeInstance.target().asClass());
            }
        }

        String[] reflectiveClasses = Stream
                .of(reflectiveClassCollector.stream(), reflectiveTypeCollector.stream(), SCHEMA_MAPPING_CLASSES.stream())
                .flatMap(Function.identity()).map(c -> c.toString()).toArray(String[]::new);
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, reflectiveClasses));

        for (Type reflectiveHierarchyType : reflectiveHierarchyCollector) {
            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(reflectiveHierarchyType));
        }
    }

    private static void addReflectiveClass(IndexView index, Set<DotName> reflectiveClassCollector,
            Set<DotName> reflectiveTypeCollector, ClassInfo classInfo) {
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
            if (superClassType instanceof ClassType) {
                superClassType = index.getClassByName(superClassType.name()).superClassType();
            } else if (superClassType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = superClassType.asParameterizedType();
                for (Type typeArgument : parameterizedType.arguments()) {
                    addReflectiveType(index, reflectiveTypeCollector, typeArgument);
                }
                superClassType = parameterizedType.owner();
            }
        }
    }

    private static void addReflectiveType(IndexView index, Set<DotName> reflectiveTypeCollector, Type type) {
        if (type instanceof VoidType || type instanceof PrimitiveType || type instanceof UnresolvedTypeVariable) {
            return;
        } else if (type instanceof ClassType) {
            if (skipClass(type.name(), reflectiveTypeCollector)) {
                return;
            }

            reflectiveTypeCollector.add(type.name());
        } else if (type instanceof ArrayType) {
            addReflectiveType(index, reflectiveTypeCollector, type.asArrayType().component());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = type.asParameterizedType();
            addReflectiveType(index, reflectiveTypeCollector, parameterizedType.owner());
            for (Type typeArgument : parameterizedType.arguments()) {
                addReflectiveType(index, reflectiveTypeCollector, typeArgument);
            }
        }
    }

    private static boolean skipClass(DotName name, Set<DotName> processedClasses) {
        return name.toString().startsWith("java.") || processedClasses.contains(name);
    }
}
