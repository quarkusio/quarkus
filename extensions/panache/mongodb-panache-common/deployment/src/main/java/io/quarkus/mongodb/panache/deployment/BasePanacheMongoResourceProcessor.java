package io.quarkus.mongodb.panache.deployment;

import static io.quarkus.deployment.util.JandexUtil.resolveTypeParameters;
import static org.jboss.jandex.DotName.createSimple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.mongodb.deployment.MongoClientNameBuildItem;
import io.quarkus.mongodb.deployment.MongoUnremovableClientsBuildItem;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoRecorder;
import io.quarkus.mongodb.panache.ProjectionFor;
import io.quarkus.mongodb.panache.jackson.ObjectIdDeserializer;
import io.quarkus.mongodb.panache.jackson.ObjectIdSerializer;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;

public abstract class BasePanacheMongoResourceProcessor {
    public static final DotName BSON_ID = createSimple(BsonId.class.getName());
    public static final DotName BSON_IGNORE = createSimple(BsonIgnore.class.getName());
    public static final DotName BSON_PROPERTY = createSimple(BsonProperty.class.getName());
    public static final DotName MONGO_ENTITY = createSimple(MongoEntity.class.getName());
    public static final DotName OBJECT_ID = createSimple(ObjectId.class.getName());
    public static final DotName PROJECTION_FOR = createSimple(ProjectionFor.class.getName());

    @BuildStep
    public void buildImperative(CombinedIndexBuildItem index, ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        processTypes(index, transformers, reflectiveClass, propertyMappingClass, getImperativeTypeBundle(),
                createRepositoryEnhancer(index, methodCustomizers), createEntityEnhancer(index, methodCustomizers));
    }

    @BuildStep
    public void buildReactive(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {
        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        processTypes(index, transformers, reflectiveClass, propertyMappingClass, getReactiveTypeBundle(),
                createReactiveRepositoryEnhancer(index, methodCustomizers),
                createReactiveEntityEnhancer(index, methodCustomizers));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    protected void buildReplacementMap(List<PropertyMappingClassBuildStep> propertyMappingClasses, CombinedIndexBuildItem index,
            PanacheMongoRecorder recorder) {
        Map<String, Map<String, String>> replacementMap = new ConcurrentHashMap<>();
        for (PropertyMappingClassBuildStep classToMap : propertyMappingClasses) {
            DotName dotName = createSimple(classToMap.getClassName());
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
            AnnotationInstance bsonProperty = field.annotation(BSON_PROPERTY);
            if (bsonProperty != null) {
                replacementMap.put(field.name(), bsonProperty.value().asString());
            }
        }
        for (MethodInfo method : classInfo.methods()) {
            if (method.name().startsWith("get")) {
                // we try to replace also for getter
                AnnotationInstance bsonProperty = method.annotation(BSON_PROPERTY);
                if (bsonProperty != null) {
                    String fieldName = JavaBeanUtil.decapitalize(method.name().substring(3));
                    replacementMap.put(fieldName, bsonProperty.value().asString());
                }
            }
        }
        return replacementMap.isEmpty() ? Collections.emptyMap() : replacementMap;
    }

    protected abstract PanacheEntityEnhancer<?> createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers);

    protected abstract PanacheEntityEnhancer<?> createReactiveEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers);

    protected abstract PanacheRepositoryEnhancer createReactiveRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers);

    protected abstract PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers);

    private void extractMappings(Map<String, String> classPropertyMapping, ClassInfo target, CombinedIndexBuildItem index) {
        for (FieldInfo fieldInfo : target.fields()) {
            if (fieldInfo.hasAnnotation(BSON_PROPERTY)) {
                AnnotationInstance bsonProperty = fieldInfo.annotation(BSON_PROPERTY);
                classPropertyMapping.put(fieldInfo.name(), bsonProperty.value().asString());
            }
        }
        for (MethodInfo methodInfo : target.methods()) {
            if (methodInfo.hasAnnotation(BSON_PROPERTY)) {
                AnnotationInstance bsonProperty = methodInfo.annotation(BSON_PROPERTY);
                classPropertyMapping.put(methodInfo.name(), bsonProperty.value().asString());
            }
        }

        // climb up the hierarchy of types
        if (!target.superClassType().name().equals(JandexUtil.DOTNAME_OBJECT)) {
            Type superType = target.superClassType();
            ClassInfo superClass = index.getIndex().getClassByName(superType.name());
            extractMappings(classPropertyMapping, superClass, index);
        }
    }

    @BuildStep
    protected PanacheEntityClassesBuildItem findEntityClasses(List<PanacheMongoEntityClassBuildItem> entityClasses) {
        if (!entityClasses.isEmpty()) {
            Set<String> ret = new HashSet<>();
            for (PanacheMongoEntityClassBuildItem entityClass : entityClasses) {
                ret.add(entityClass.get().name().toString());
            }
            return new PanacheEntityClassesBuildItem(ret);
        }
        return null;
    }

    protected abstract TypeBundle getImperativeTypeBundle();

    protected abstract TypeBundle getReactiveTypeBundle();

    @BuildStep
    protected void handleProjectionFor(CombinedIndexBuildItem index,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        // manage @BsonProperty for the @ProjectionFor annotation
        Map<DotName, Map<String, String>> propertyMapping = new HashMap<>();
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(PROJECTION_FOR)) {
            Type targetClass = annotationInstance.value().asClass();
            ClassInfo target = index.getIndex().getClassByName(targetClass.name());
            Map<String, String> classPropertyMapping = new HashMap<>();
            extractMappings(classPropertyMapping, target, index);
            propertyMapping.put(targetClass.name(), classPropertyMapping);
        }
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(PROJECTION_FOR)) {
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

    @BuildStep
    public void mongoClientNames(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<MongoClientNameBuildItem> mongoClientName) {
        Set<String> values = new HashSet<>();
        IndexView indexView = applicationArchivesBuildItem.getRootArchive().getIndex();
        Collection<AnnotationInstance> instances = indexView.getAnnotations(MONGO_ENTITY);
        for (AnnotationInstance annotation : instances) {
            AnnotationValue clientName = annotation.value("clientName");
            if ((clientName != null) && !clientName.asString().isEmpty()) {
                values.add(clientName.asString());
            }
        }
        for (String value : values) {
            // we don't want the qualifier @MongoClientName qualifier added
            // as these clients will only be looked up programmatically via name
            // see MongoOperations#mongoClient
            mongoClientName.produce(new MongoClientNameBuildItem(value, false));
        }
    }

    protected void processEntities(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            PanacheEntityEnhancer<?> entityEnhancer, TypeBundle typeBundle) {

        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(typeBundle.entityBase().dotName())) {
            if (classInfo.name().equals(typeBundle.entity().dotName())) {
                continue;
            }
            if (modelClasses.add(classInfo.name().toString()))
                entityEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(typeBundle.entity().dotName())) {
            if (modelClasses.add(classInfo.name().toString()))
                entityEnhancer.collectFields(classInfo);
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, entityEnhancer));

            //register for reflection entity classes
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, modelClass));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(modelClass));
        }
    }

    protected void processRepositories(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            PanacheRepositoryEnhancer repositoryEnhancer, TypeBundle typeBundle) {

        Set<String> daoClasses = new HashSet<>();
        Set<Type> daoTypeParameters = new HashSet<>();
        DotName dotName = typeBundle.repositoryBase().dotName();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(dotName)) {
            // Skip PanacheMongoRepository and abstract repositories
            if (classInfo.name().equals(typeBundle.repository().dotName()) || repositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    resolveTypeParameters(classInfo.name(), typeBundle.repositoryBase().dotName(), index.getIndex()));
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(typeBundle.repository().dotName())) {
            if (repositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    resolveTypeParameters(classInfo.name(), typeBundle.repositoryBase().dotName(), index.getIndex()));
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, repositoryEnhancer));
        }

        for (Type parameterType : daoTypeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(parameterType.name().toString()));
        }
    }

    protected void processTypes(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            TypeBundle typeBundle, PanacheRepositoryEnhancer repositoryEnhancer,
            PanacheEntityEnhancer<?> entityEnhancer) {

        processRepositories(index, transformers, reflectiveClass, propertyMappingClass,
                repositoryEnhancer, typeBundle);
        processEntities(index, transformers, reflectiveClass, propertyMappingClass,
                entityEnhancer, typeBundle);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, OBJECT_ID.toString());
    }

    @BuildStep
    protected void registerJacksonSerDeser(BuildProducer<JacksonModuleBuildItem> customSerDeser) {
        customSerDeser.produce(
                new JacksonModuleBuildItem.Builder("ObjectIdModule")
                        .add(ObjectIdSerializer.class.getName(),
                                ObjectIdDeserializer.class.getName(),
                                ObjectId.class.getName())
                        .build());
    }

    @BuildStep
    protected void registerJsonbSerDeser(BuildProducer<JsonbSerializerBuildItem> jsonbSerializers,
            BuildProducer<JsonbDeserializerBuildItem> jsonbDeserializers) {
        jsonbSerializers
                .produce(new JsonbSerializerBuildItem(io.quarkus.mongodb.panache.jsonb.ObjectIdSerializer.class.getName()));
        jsonbDeserializers
                .produce(new JsonbDeserializerBuildItem(io.quarkus.mongodb.panache.jsonb.ObjectIdDeserializer.class.getName()));
    }

    @BuildStep
    public void unremovableClients(BuildProducer<MongoUnremovableClientsBuildItem> unremovable) {
        unremovable.produce(new MongoUnremovableClientsBuildItem());
    }

    @BuildStep
    protected ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @BsonId) when extending PanacheMongoEntity or ReactivePanacheMongoEntity
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(BSON_ID)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getIndex(), info,
                    getImperativeTypeBundle().entity().dotName())) {
                BuildException be = new BuildException("You provide a MongoDB identifier via @BsonId inside '" + info.name() +
                        "' but one is already provided by PanacheMongoEntity, " +
                        "your class should extend PanacheMongoEntityBase instead, or use the id provided by PanacheMongoEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            } else if (JandexUtil.isSubclassOf(index.getIndex(), info,
                    getReactiveTypeBundle().entity().dotName())) {
                BuildException be = new BuildException("You provide a MongoDB identifier via @BsonId inside '" + info.name() +
                        "' but one is already provided by ReactivePanacheMongoEntity, " +
                        "your class should extend ReactivePanacheMongoEntityBase instead, or use the id provided by ReactivePanacheMongoEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }
}
