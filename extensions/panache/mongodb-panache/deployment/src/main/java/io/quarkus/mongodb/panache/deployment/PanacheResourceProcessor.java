package io.quarkus.mongodb.panache.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRecorder;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.ProjectionFor;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheResourceProcessor {
    // blocking types
    static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheMongoRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheMongoRepository.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheMongoEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheMongoEntity.class.getName());

    private static final DotName DOTNAME_PROJECTION_FOR = DotName.createSimple(ProjectionFor.class.getName());
    private static final DotName DOTNAME_BSON_PROPERTY = DotName.createSimple(BsonProperty.class.getName());

    // reactive types (Mutiny)
    static final DotName DOTNAME_MUTINY_PANACHE_REPOSITORY_BASE = DotName
            .createSimple(ReactivePanacheMongoRepositoryBase.class.getName());
    private static final DotName DOTNAME_MUTINY_PANACHE_REPOSITORY = DotName
            .createSimple(ReactivePanacheMongoRepository.class.getName());
    static final DotName DOTNAME_MUTINY_PANACHE_ENTITY_BASE = DotName
            .createSimple(ReactivePanacheMongoEntityBase.class.getName());
    private static final DotName DOTNAME_MUTINY_PANACHE_ENTITY = DotName
            .createSimple(ReactivePanacheMongoEntity.class.getName());

    private static final DotName DOTNAME_OBJECT_ID = DotName.createSimple(ObjectId.class.getName());

    private static final DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.MONGODB_PANACHE);
    }

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.MONGODB_PANACHE);
    }

    @BuildStep
    void registerJsonbSerDeser(BuildProducer<JsonbSerializerBuildItem> jsonbSerializers,
            BuildProducer<JsonbDeserializerBuildItem> jsonbDeserializers) {
        jsonbSerializers
                .produce(new JsonbSerializerBuildItem(io.quarkus.mongodb.panache.jsonb.ObjectIdSerializer.class.getName()));
        jsonbDeserializers
                .produce(new JsonbDeserializerBuildItem(io.quarkus.mongodb.panache.jsonb.ObjectIdDeserializer.class.getName()));
    }

    @BuildStep
    void registerJacksonSerDeser(BuildProducer<JacksonModuleBuildItem> customSerDeser) {
        customSerDeser.produce(
                new JacksonModuleBuildItem.Builder("ObjectIdModule")
                        .add(io.quarkus.mongodb.panache.jackson.ObjectIdSerializer.class.getName(),
                                io.quarkus.mongodb.panache.jackson.ObjectIdDeserializer.class.getName(),
                                ObjectId.class.getName())
                        .build());
    }

    @BuildStep
    ReflectiveHierarchyBuildItem registerForReflection(CombinedIndexBuildItem index) {
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        IndexingUtil.indexClass(ObjectId.class.getName(), indexer, index.getIndex(), additionalIndex,
                PanacheResourceProcessor.class.getClassLoader());
        CompositeIndex compositeIndex = CompositeIndex.create(index.getIndex(), indexer.complete());
        Type type = Type.create(DOTNAME_OBJECT_ID, Type.Kind.CLASS);
        return new ReflectiveHierarchyBuildItem(type, compositeIndex);
    }

    @BuildStep
    void unremovableProducers(BuildProducer<UnremovableBeanBuildItem> unremovable,
            BeanArchiveIndexBuildItem beanArchiveIndex) {
        unremovable.produce(
                new UnremovableBeanBuildItem(
                        new UnremovableBeanBuildItem.BeanClassNameExclusion(
                                "io.quarkus.mongodb.runtime.MongoClientProducer")));

    }

    @BuildStep
    void buildImperative(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<PanacheEntityClassesBuildItem> entityClasses,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        PanacheMongoRepositoryEnhancer daoEnhancer = new PanacheMongoRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        Set<Type> daoTypeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheMongoRepository and abstract repositories
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY) || PanacheRepositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    JandexUtil.resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex()));
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY)) {
            if (PanacheRepositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    JandexUtil.resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex()));
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        for (Type parameterType : daoTypeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(parameterType.name().toString()));
        }

        PanacheMongoEntityEnhancer modelEnhancer = new PanacheMongoEntityEnhancer(index.getIndex(), methodCustomizers);
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            if (classInfo.name().equals(DOTNAME_PANACHE_ENTITY)) {
                continue;
            }
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY)) {
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));

            //register for reflection entity classes
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, modelClass));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(modelClass));
        }
        if (!modelClasses.isEmpty()) {
            entityClasses.produce(new PanacheEntityClassesBuildItem(modelClasses));
        }

        if (!modelEnhancer.entities.isEmpty()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(
                    modelEnhancer.getModelInfo());
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }

    @BuildStep
    void buildMutiny(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        ReactivePanacheMongoRepositoryEnhancer daoEnhancer = new ReactivePanacheMongoRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        Set<Type> daoTypeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_MUTINY_PANACHE_REPOSITORY_BASE)) {
            // Skip ReactivePanacheMongoRepository and abstract repositories
            if (classInfo.name().equals(DOTNAME_MUTINY_PANACHE_REPOSITORY)
                    || PanacheRepositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    JandexUtil.resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex()));
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_MUTINY_PANACHE_REPOSITORY)) {
            if (PanacheRepositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    JandexUtil.resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex()));
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        for (Type parameterType : daoTypeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(parameterType.name().toString()));
        }

        ReactivePanacheMongoEntityEnhancer modelEnhancer = new ReactivePanacheMongoEntityEnhancer(index.getIndex(),
                methodCustomizers);
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_MUTINY_PANACHE_ENTITY_BASE)) {
            if (classInfo.name().equals(DOTNAME_MUTINY_PANACHE_ENTITY)) {
                continue;
            }
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_MUTINY_PANACHE_ENTITY)) {
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));

            //register for reflection entity classes
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, modelClass));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(modelClass));
        }

        if (!modelEnhancer.entities.isEmpty()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(
                    modelEnhancer.getModelInfo());
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }

    @BuildStep
    void handleProjectionFor(CombinedIndexBuildItem index,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        // manage @BsonProperty for the @ProjectionFor annotation
        Map<DotName, Map<String, String>> propertyMapping = new HashMap<>();
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_PROJECTION_FOR)) {
            Type targetClass = annotationInstance.value().asClass();
            ClassInfo target = index.getIndex().getClassByName(targetClass.name());
            Map<String, String> classPropertyMapping = new HashMap<>();
            extractMappings(classPropertyMapping, target, index);
            propertyMapping.put(targetClass.name(), classPropertyMapping);
        }
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_PROJECTION_FOR)) {
            Type targetClass = annotationInstance.value().asClass();
            Map<String, String> targetPropertyMapping = propertyMapping.get(targetClass.name());
            if (targetPropertyMapping != null && !targetPropertyMapping.isEmpty()) {
                ClassInfo info = annotationInstance.target().asClass();
                ProjectionForEnhancer fieldEnhancer = new ProjectionForEnhancer(targetPropertyMapping);
                transformers.produce(new BytecodeTransformerBuildItem(info.name().toString(), fieldEnhancer));
            }

            // Register for building the property mapping cache
            propertyMappingClass
                    .produce(new PropertyMappingClassBuildStep(targetClass.name().toString(),
                            annotationInstance.target().asClass().name().toString()));
        }
    }

    private void extractMappings(Map<String, String> classPropertyMapping, ClassInfo target, CombinedIndexBuildItem index) {
        for (FieldInfo fieldInfo : target.fields()) {
            if (fieldInfo.hasAnnotation(DOTNAME_BSON_PROPERTY)) {
                AnnotationInstance bsonProperty = fieldInfo.annotation(DOTNAME_BSON_PROPERTY);
                classPropertyMapping.put(fieldInfo.name(), bsonProperty.value().asString());
            }
        }
        for (MethodInfo methodInfo : target.methods()) {
            if (methodInfo.hasAnnotation(DOTNAME_BSON_PROPERTY)) {
                AnnotationInstance bsonProperty = methodInfo.annotation(DOTNAME_BSON_PROPERTY);
                classPropertyMapping.put(methodInfo.name(), bsonProperty.value().asString());
            }
        }

        // climb up the hierarchy of types
        if (!target.superClassType().name().equals(DOTNAME_OBJECT)) {
            Type superType = target.superClassType();
            ClassInfo superClass = index.getIndex().getClassByName(superType.name());
            extractMappings(classPropertyMapping, superClass, index);
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildReplacementMap(List<PropertyMappingClassBuildStep> propertyMappingClasses, CombinedIndexBuildItem index,
            PanacheMongoRecorder recorder) {
        Map<String, Map<String, String>> replacementMap = new ConcurrentHashMap<>();
        for (PropertyMappingClassBuildStep classToMap : propertyMappingClasses) {
            DotName dotName = DotName.createSimple(classToMap.getClassName());
            ClassInfo classInfo = index.getIndex().getClassByName(dotName);
            if (classInfo != null) {
                // only compute field replacement for types inside the index
                Map<String, String> classReplacementMap = replacementMap.computeIfAbsent(classToMap.getClassName(),
                        className -> computeReplacement(classInfo));
                if (classToMap.getAliasClassName() != null) {
                    // also register the replacement map for the projection classes
                    replacementMap.put(classToMap.getAliasClassName(), classReplacementMap);
                }
            }
        }

        recorder.setReplacementCache(replacementMap);
    }

    private Map<String, String> computeReplacement(ClassInfo classInfo) {
        Map<String, String> replacementMap = new HashMap<>();
        for (FieldInfo field : classInfo.fields()) {
            AnnotationInstance bsonProperty = field.annotation(DOTNAME_BSON_PROPERTY);
            if (bsonProperty != null) {
                replacementMap.put(field.name(), bsonProperty.value().asString());
            }
        }
        for (MethodInfo method : classInfo.methods()) {
            if (method.name().startsWith("get")) {
                // we try to replace also for getter
                AnnotationInstance bsonProperty = method.annotation(DOTNAME_BSON_PROPERTY);
                if (bsonProperty != null) {
                    String fieldName = JavaBeanUtil.decapitalize(method.name().substring(3));
                    replacementMap.put(fieldName, bsonProperty.value().asString());
                }
            }
        }
        return replacementMap.isEmpty() ? Collections.emptyMap() : replacementMap;
    }

}
