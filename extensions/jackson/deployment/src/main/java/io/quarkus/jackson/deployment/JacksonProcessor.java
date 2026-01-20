package io.quarkus.jackson.deployment;

import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Reflection2Gizmo;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.jackson.JacksonMixin;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.jackson.runtime.ConfigurationCustomizer;
import io.quarkus.jackson.runtime.JacksonBuildTimeConfig;
import io.quarkus.jackson.runtime.JacksonRecorder;
import io.quarkus.jackson.runtime.JacksonSupport;
import io.quarkus.jackson.runtime.ObjectMapperProducer;
import io.quarkus.jackson.runtime.VertxHybridPoolObjectMapperCustomizer;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;

public class JacksonProcessor {

    private static final DotName JSON_DESERIALIZE = DotName.createSimple(JsonDeserialize.class.getName());

    private static final DotName JSON_SERIALIZE = DotName.createSimple(JsonSerialize.class.getName());

    private static final DotName JSON_AUTO_DETECT = DotName.createSimple(JsonAutoDetect.class.getName());

    private static final DotName JSON_TYPE_ID_RESOLVER = DotName.createSimple(JsonTypeIdResolver.class.getName());
    private static final DotName JSON_SUBTYPES = DotName.createSimple(JsonSubTypes.class.getName());
    private static final DotName JSON_CREATOR = DotName.createSimple("com.fasterxml.jackson.annotation.JsonCreator");

    private static final DotName JSON_NAMING = DotName.createSimple("com.fasterxml.jackson.databind.annotation.JsonNaming");

    private static final DotName JSON_IDENTITY_INFO = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIdentityInfo");

    private static final DotName BUILDER_VOID = DotName.createSimple(Void.class.getName());

    private static final String TIME_MODULE = "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule";

    private static final String JDK8_MODULE = "com.fasterxml.jackson.datatype.jdk8.Jdk8Module";

    private static final String PARAMETER_NAMES_MODULE = "com.fasterxml.jackson.module.paramnames.ParameterNamesModule";
    private static final DotName JACKSON_MIXIN = DotName.createSimple(JacksonMixin.class.getName());

    private static final MethodDesc OBJECT_MAPPER_REGISTER_MODULE_METHOD_DESC = MethodDesc.of(ObjectMapper.class,
            "registerModule", ObjectMapper.class, Module.class);

    // this list can probably be enriched with more modules
    private static final List<String> MODULES_NAMES_TO_AUTO_REGISTER = Arrays.asList(TIME_MODULE, JDK8_MODULE,
            PARAMETER_NAMES_MODULE);
    private static final String CLASS_NAME = JacksonProcessor.class.getName();
    private static final String MIXIN_VALUE_PREFIX = CLASS_NAME + " @" + JACKSON_MIXIN + " value of ";
    private static final String ANNOTATED_WITH_MIXIN = CLASS_NAME + " annotated with @" + JACKSON_MIXIN;
    private static final String JSON_SUBTYPES_VALUE = CLASS_NAME + " @" + JSON_SUBTYPES + " value";
    private static final String JSON_IDENTITY_RESOLVER_DEFAULT_VALUE = CLASS_NAME + " @" + JSON_IDENTITY_INFO
            + " resolver default value";
    private static final String JSON_IDENTITY_RESOLVER = CLASS_NAME + " @" + JSON_IDENTITY_INFO + " resolver";
    private static final String JSON_IDENTITY_GENERATOR = CLASS_NAME + " @" + JSON_IDENTITY_INFO + " generator";
    private static final String JSON_NAMING_VALUE = CLASS_NAME + " @" + JSON_NAMING + " value";
    private static final String TYPE_ID_RESOLVER_VALUE = CLASS_NAME + " @" + JSON_TYPE_ID_RESOLVER + " value";
    private static final String ANNOTATED_WITH_AUTO_DETECT = CLASS_NAME + " annotated with @" + JSON_AUTO_DETECT;
    private static final String SERIALIZE_NULLS_USING = CLASS_NAME + " @" + JSON_SERIALIZE + " nullsUsing";
    private static final String SERIALIZE_USING = CLASS_NAME + " @" + JSON_SERIALIZE + " using";
    private static final String SERIALIZE_KEY_USING = CLASS_NAME + " @" + JSON_SERIALIZE + " keyUsing";
    private static final String SERIALIZE_CONTENT_USING = CLASS_NAME + " @" + JSON_SERIALIZE + " contentUsing";
    private static final String DESERIALIZE_USING = CLASS_NAME + " @" + JSON_DESERIALIZE + " using";
    private static final String DESERIALIZE_KEY_USING = CLASS_NAME + " @" + JSON_DESERIALIZE + " keyUsing";
    private static final String DESERIALIZE_CONTENT_USING = CLASS_NAME + " @" + JSON_DESERIALIZE + " contentUsing";

    private static final String SIMPLE_CLASS_NAME = JacksonProcessor.class.getSimpleName();
    private static final String ANNOTATED_WITH_TYPE_ID_RESOLVER = SIMPLE_CLASS_NAME + " annotated with @"
            + JSON_TYPE_ID_RESOLVER;
    private static final String DESERIALIZE_BUILDER_OF = SIMPLE_CLASS_NAME + " @" + JSON_DESERIALIZE + " builder of ";
    private static final String ANNOTATED_WITH_DESERIALIZE = SIMPLE_CLASS_NAME + " annotated with @" + JSON_DESERIALIZE;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    List<IgnoreJsonDeserializeClassBuildItem> ignoreJsonDeserializeClassBuildItems;

    @BuildStep
    void unremovable(Capabilities capabilities, BuildProducer<UnremovableBeanBuildItem> producer,
            BuildProducer<AdditionalBeanBuildItem> additionalProducer) {
        additionalProducer.produce(AdditionalBeanBuildItem.unremovableOf(ConfigurationCustomizer.class));

        if (capabilities.isPresent(Capability.VERTX_CORE)) {
            producer.produce(UnremovableBeanBuildItem.beanTypes(ObjectMapper.class));
            additionalProducer.produce(AdditionalBeanBuildItem.unremovableOf(VertxHybridPoolObjectMapperCustomizer.class));
        }
    }

    @BuildStep
    void register(
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("com.fasterxml.jackson.databind.ser.std.SqlDateSerializer",
                        "com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer",
                        "com.fasterxml.jackson.databind.deser.std.DateDeserializers$SqlDateDeserializer",
                        "com.fasterxml.jackson.databind.deser.std.DateDeserializers$TimestampDeserializer",
                        "com.fasterxml.jackson.annotation.SimpleObjectIdResolver")
                        .reason(CLASS_NAME)
                        .methods().build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        "com.fasterxml.jackson.databind.ser.std.ClassSerializer",
                        "com.fasterxml.jackson.databind.ext.CoreXMLSerializers",
                        "com.fasterxml.jackson.databind.ext.CoreXMLDeserializers")
                        .reason(CLASS_NAME)
                        .constructors()
                        .build());

        if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().anyMatch(
                x -> x.getGroupId().equals("com.fasterxml.jackson.module")
                        && x.getArtifactId().equals("jackson-module-jaxb-annotations"))) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder("com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector")
                            .reason(CLASS_NAME)
                            .methods().build());
        }

        IndexView index = combinedIndexBuildItem.getIndex();

        // TODO: @JsonDeserialize is only supported as a class annotation - we should support the others as well

        Set<DotName> ignoredDotNames = new HashSet<>();
        for (IgnoreJsonDeserializeClassBuildItem ignoreJsonDeserializeClassBuildItem : ignoreJsonDeserializeClassBuildItems) {
            ignoredDotNames.addAll(ignoreJsonDeserializeClassBuildItem.getDotNames());
        }

        // handle the various @JsonDeserialize cases
        for (AnnotationInstance deserializeInstance : index.getAnnotations(JSON_DESERIALIZE)) {
            AnnotationTarget annotationTarget = deserializeInstance.target();
            if (CLASS.equals(annotationTarget.kind())) {
                DotName dotName = annotationTarget.asClass().name();
                if (!ignoredDotNames.contains(dotName)) {
                    addReflectiveHierarchyClass(ANNOTATED_WITH_DESERIALIZE, dotName, reflectiveHierarchyClass);
                }

                AnnotationValue annotationValue = deserializeInstance.value("builder");
                if (null != annotationValue && AnnotationValue.Kind.CLASS.equals(annotationValue.kind())) {
                    DotName builderClassName = annotationValue.asClass().name();
                    if (!BUILDER_VOID.equals(builderClassName)) {
                        addReflectiveHierarchyClass(DESERIALIZE_BUILDER_OF + dotName,
                                builderClassName, reflectiveHierarchyClass);
                    }
                }
            }
            registerUsingValuesForReflection(reflectiveClass, deserializeInstance, true);
        }

        // handle the various @JsonSerialize cases
        for (AnnotationInstance serializeInstance : index.getAnnotations(JSON_SERIALIZE)) {
            registerUsingValuesForReflection(reflectiveClass, serializeInstance, false);
            AnnotationValue nullsUsingValue = serializeInstance.value("nullsUsing");
            if (nullsUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(nullsUsingValue.asClass().name().toString())
                                .reason(SERIALIZE_NULLS_USING)
                                .build());
            }
        }

        final var autoDetects = index.getAnnotations(JSON_AUTO_DETECT).stream()
                .filter(ai -> ai.target().kind() == CLASS)
                .map(ai -> ai.target().asClass().name().toString())
                .toList();
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(autoDetects)
                .reason(ANNOTATED_WITH_AUTO_DETECT)
                .methods().fields().build());

        // Register @JsonTypeIdResolver implementations for reflection.
        // Note: @JsonTypeIdResolver is, simply speaking, the "dynamic version" of @JsonSubTypes, i.e. sub-types are
        // dynamically identified by Jackson's `TypeIdResolver.typeFromId()`, which returns sub-types of the annotated
        // class. Means: the referenced `TypeIdResolver` _and_ all sub-types of the annotated class must be registered
        // for reflection.
        for (AnnotationInstance resolverInstance : index.getAnnotations(JSON_TYPE_ID_RESOLVER)) {
            AnnotationValue value = resolverInstance.value("value");
            if (value != null) {
                // Add the type-id-resolver class
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(value.asClass().name().toString()).methods().fields()
                                .reason(TYPE_ID_RESOLVER_VALUE)
                                .build());
                if (resolverInstance.target().kind() == CLASS) {
                    // Add the whole hierarchy of the annotated class
                    addReflectiveHierarchyClass(ANNOTATED_WITH_TYPE_ID_RESOLVER,
                            resolverInstance.target().asClass().name(), reflectiveHierarchyClass);
                }
            }
        }

        // make sure we register the constructors and methods marked with @JsonCreator for reflection
        for (AnnotationInstance creatorInstance : index.getAnnotations(JSON_CREATOR)) {
            if (METHOD == creatorInstance.target().kind()) {
                reflectiveMethod
                        .produce(new ReflectiveMethodBuildItem(CLASS_NAME, creatorInstance.target().asMethod()));
            }
        }

        // register @JsonNaming strategy implementations for reflection
        final var namingValueClasses = index.getAnnotations(JSON_NAMING).stream()
                .map(ai -> ai.value("value"))
                .filter(Objects::nonNull)
                .map(av -> av.asClass().name().toString())
                .toList();
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(namingValueClasses)
                .reason(JSON_NAMING_VALUE)
                .methods().fields().build());

        // register @JsonIdentityInfo strategy implementations for reflection
        Collection<String> identityGenerators = new ArrayList<>();
        Collection<String> identityResolvers = new ArrayList<>();
        boolean hasRegisteredDefault = false;
        for (AnnotationInstance jsonIdentityInfoInstance : index.getAnnotations(JSON_IDENTITY_INFO)) {
            AnnotationValue generatorValue = jsonIdentityInfoInstance.value("generator");
            AnnotationValue resolverValue = jsonIdentityInfoInstance.value("resolver");
            if (generatorValue != null) {
                identityGenerators.add(generatorValue.asClass().name().toString());
            }
            if (resolverValue != null) {
                identityResolvers.add(resolverValue.asClass().name().toString());
            } else {
                if (!hasRegisteredDefault) {
                    // Registering since SimpleObjectIdResolver is the default value of @JsonIdentityInfo.resolver
                    reflectiveClass.produce(ReflectiveClassBuildItem.builder(SimpleObjectIdResolver.class)
                            .reason(JSON_IDENTITY_RESOLVER_DEFAULT_VALUE)
                            .methods().fields().build());
                    hasRegisteredDefault = true;
                }
            }
        }
        if (!identityGenerators.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(identityGenerators)
                    .reason(JSON_IDENTITY_GENERATOR)
                    .methods().fields().build());
        }
        if (!identityResolvers.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(identityResolvers)
                    .reason(JSON_IDENTITY_RESOLVER)
                    .methods().fields().build());
        }

        // register @JsonSubTypes.Type values for reflection
        Set<String> subTypeTypesNames = new HashSet<>();
        for (AnnotationInstance subTypeInstance : index.getAnnotations(JSON_SUBTYPES)) {
            AnnotationValue subTypeValue = subTypeInstance.value();
            if (subTypeValue != null) {
                for (AnnotationInstance subTypeTypeInstance : subTypeValue.asNestedArray()) {
                    AnnotationValue subTypeTypeValue = subTypeTypeInstance.value();
                    if (subTypeTypeValue != null) {
                        subTypeTypesNames.add(subTypeTypeValue.asClass().name().toString());
                    }
                }

            }
        }
        if (!subTypeTypesNames.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(subTypeTypesNames)
                    .reason(JSON_SUBTYPES_VALUE)
                    .methods().fields().build());
        }

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBeans.produce(new AdditionalBeanBuildItem(ObjectMapperProducer.class));
    }

    private void registerUsingValuesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            AnnotationInstance annotationInstance, boolean forDeserialization) {
        AnnotationValue usingValue = annotationInstance.value("using");
        if (usingValue != null) {
            // the Serializers are constructed internally by Jackson using a no-args constructor
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(usingValue.asClass().name().toString())
                    .reason(forDeserialization ? DESERIALIZE_USING : SERIALIZE_USING)
                    .build());
        }
        AnnotationValue keyUsingValue = annotationInstance.value("keyUsing");
        if (keyUsingValue != null) {
            // the Deserializers are constructed internally by Jackson using a no-args constructor
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(keyUsingValue.asClass().name().toString())
                    .reason(forDeserialization ? DESERIALIZE_KEY_USING : SERIALIZE_KEY_USING)
                    .build());
        }
        AnnotationValue contentUsingValue = annotationInstance.value("contentUsing");
        if (contentUsingValue != null) {
            // the Deserializers are constructed internally by Jackson using a no-args constructor
            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(contentUsingValue.asClass().name().toString())
                            .reason(forDeserialization ? DESERIALIZE_CONTENT_USING : SERIALIZE_CONTENT_USING)
                            .build());
        }
    }

    private void addReflectiveHierarchyClass(String reason, DotName className,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass) {
        reflectiveHierarchyClass.produce(ReflectiveHierarchyBuildItem.builder(className)
                .source(reason)
                .build());
    }

    @BuildStep
    void autoRegisterModules(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        for (String module : MODULES_NAMES_TO_AUTO_REGISTER) {
            registerModuleIfOnClassPath(module, classPathJacksonModules);
        }
    }

    private void registerModuleIfOnClassPath(String moduleClassName,
            BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        if (QuarkusClassLoader.isClassPresentAtRuntime(moduleClassName)) {
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(moduleClassName));
        }
    }

    // Generate a ObjectMapperCustomizer bean that registers each serializer / deserializer as well as detected modules with the ObjectMapper
    @BuildStep
    void generateCustomizer(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            List<JacksonModuleBuildItem> jacksonModules, List<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {

        if (jacksonModules.isEmpty() && classPathJacksonModules.isEmpty()) {
            return;
        }

        ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
        Gizmo g = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);
        g.class_("io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer", cc -> {
            cc.implements_(ObjectMapperCustomizer.class);
            cc.defaultConstructor();
            cc.addAnnotation(Singleton.class);
            cc.method("customize", mc -> {
                ParamVar objectMapperParam = mc.parameter("objectMapper", ObjectMapper.class);
                mc.returning(void.class);
                mc.body(bc -> {
                    ClassDesc simpleModuleClassDesc = Reflection2Gizmo.classDescOf(SimpleModule.class);
                    for (JacksonModuleBuildItem jacksonModule : jacksonModules) {
                        if (jacksonModule.getItems().isEmpty()) {
                            continue;
                        }

                        /*
                         * Create code similar to the following:
                         *
                         * SimpleModule module = new SimpleModule("somename");
                         * module.addSerializer(Foo.class, new FooSerializer());
                         * module.addDeserializer(Foo.class, new FooDeserializer());
                         * objectMapper.registerModule(module);
                         */

                        LocalVar simpleModuleInstance = bc.localVar("simpleModule",
                                bc.new_(SimpleModule.class, Const.of(jacksonModule.getName())));

                        for (JacksonModuleBuildItem.Item item : jacksonModule.getItems()) {

                            LocalVar targetClass = bc.localVar("targetClass",
                                    Const.of(ClassDesc.of(item.getTargetClassName())));
                            String serializerClassName = item.getSerializerClassName();
                            if ((serializerClassName != null) && !serializerClassName.isEmpty()) {
                                ClassDesc serializerClassDesc = ClassDesc.of(serializerClassName);
                                Expr serializerInstance = bc.new_(serializerClassDesc);

                                bc.invokeVirtual(
                                        ClassMethodDesc.of(simpleModuleClassDesc, "addSerializer",
                                                MethodTypeDesc.of(simpleModuleClassDesc,
                                                        ConstantDescs.CD_Class, Reflection2Gizmo.classDescOf(
                                                                JsonSerializer.class))),
                                        simpleModuleInstance, targetClass, serializerInstance);

                            }

                            String deserializerClassName = item.getDeserializerClassName();
                            if ((deserializerClassName != null) && !deserializerClassName.isEmpty()) {
                                ClassDesc deserializerClassDesc = ClassDesc.of(deserializerClassName);
                                Expr deserializerInstance = bc.new_(deserializerClassDesc);

                                bc.invokeVirtual(
                                        ClassMethodDesc.of(simpleModuleClassDesc, "addDeserializer",
                                                MethodTypeDesc.of(simpleModuleClassDesc,
                                                        ConstantDescs.CD_Class, Reflection2Gizmo.classDescOf(
                                                                JsonDeserializer.class))),
                                        simpleModuleInstance, targetClass, deserializerInstance);

                            }
                        }

                        bc.invokeVirtual(
                                OBJECT_MAPPER_REGISTER_MODULE_METHOD_DESC,
                                objectMapperParam, simpleModuleInstance);

                    }

                    for (ClassPathJacksonModuleBuildItem classPathJacksonModule : classPathJacksonModules) {
                        bc.invokeVirtual(
                                OBJECT_MAPPER_REGISTER_MODULE_METHOD_DESC,
                                objectMapperParam, bc.new_(ClassDesc.of(classPathJacksonModule.getModuleClassName())));
                    }

                    bc.return_();
                });
            });
            cc.method("priority", mc -> {
                mc.returning(int.class);
                mc.body(bc -> bc.return_(ObjectMapperCustomizer.QUARKUS_CUSTOMIZER_PRIORITY));
            });
        });
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void supportMixins(JacksonRecorder recorder,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> jacksonMixins = index.getAnnotations(JACKSON_MIXIN);
        if (jacksonMixins.isEmpty()) {
            return;
        }

        Map<Class<?>, Class<?>> mixinsMap = new HashMap<>();
        Collection<String> mixinClasses = new ArrayList<>(jacksonMixins.size());
        for (AnnotationInstance instance : jacksonMixins) {
            if (instance.target().kind() != CLASS) {
                continue;
            }
            ClassInfo mixinClassInfo = instance.target().asClass();
            String mixinClassName = mixinClassInfo.name().toString();
            mixinClasses.add(mixinClassName);
            try {
                Type[] targetTypes = instance.value().asClassArray();
                if ((targetTypes == null) || targetTypes.length == 0) {
                    continue;
                }
                Class<?> mixinClass = Thread.currentThread().getContextClassLoader().loadClass(mixinClassName);
                final var targetClassNames = new ArrayList<String>(targetTypes.length);
                for (Type targetType : targetTypes) {
                    String targetClassName = targetType.name().toString();
                    targetClassNames.add(targetClassName);
                    mixinsMap.put(Thread.currentThread().getContextClassLoader().loadClass(targetClassName),
                            mixinClass);
                }
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(targetClassNames)
                        .reason(MIXIN_VALUE_PREFIX + mixinClassName)
                        .methods().fields().build());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to determine Jackson mixin usage at build", e);
            }
        }
        if (!mixinClasses.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(mixinClasses)
                    .reason(ANNOTATED_WITH_MIXIN)
                    .methods().fields().build());
        }
        if (mixinsMap.isEmpty()) {
            return;
        }
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ObjectMapperCustomizer.class)
                .scope(Singleton.class)
                .supplier(recorder.customizerSupplier(mixinsMap))
                .done());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public SyntheticBeanBuildItem jacksonSupport(JacksonRecorder recorder,
            JacksonBuildTimeConfig jacksonBuildTimeConfig) {
        return SyntheticBeanBuildItem
                .configure(JacksonSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.supplier(determinePropertyNamingStrategyClassName(jacksonBuildTimeConfig)))
                .done();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void clearCachesOnShutdown(JacksonRecorder recorder, ShutdownContextBuildItem shutdown) {
        recorder.clearCachesOnShutdown(shutdown);
    }

    private Optional<String> determinePropertyNamingStrategyClassName(JacksonBuildTimeConfig jacksonBuildTimeConfig) {
        if (jacksonBuildTimeConfig.propertyNamingStrategy().isEmpty()) {
            return Optional.empty();
        }
        var propertyNamingStrategy = jacksonBuildTimeConfig.propertyNamingStrategy().get();
        Field field;

        try {
            // let's first try and see if the value is a constant defined in PropertyNamingStrategies
            field = Reflections.findField(PropertyNamingStrategies.class, propertyNamingStrategy);
        } catch (Exception e) {
            // the provided value does not correspond to any of the defined constants, so let's see if it's actually a class name
            try {
                var clazz = Thread.currentThread().getContextClassLoader().loadClass(propertyNamingStrategy);
                if (PropertyNamingStrategy.class.isAssignableFrom(clazz)) {
                    return Optional.of(propertyNamingStrategy);

                }
                throw new RuntimeException(invalidPropertyNameStrategyValueMessage(propertyNamingStrategy));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(invalidPropertyNameStrategyValueMessage(propertyNamingStrategy));
            }
        }

        try {
            // we have a matching field, so let's see if the type is correct
            Class<?> clazz = field.get(null).getClass();
            if (PropertyNamingStrategy.class.isAssignableFrom(clazz)) {
                return Optional.of(clazz.getName());
            }
            throw new RuntimeException(invalidPropertyNameStrategyValueMessage(propertyNamingStrategy));
        } catch (IllegalAccessException e) {
            // shouldn't ever happen
            throw new RuntimeException(invalidPropertyNameStrategyValueMessage(propertyNamingStrategy));
        }
    }

    private static String invalidPropertyNameStrategyValueMessage(String propertyNamingStrategy) {
        return "Unable to determine the property naming strategy for value '" + propertyNamingStrategy
                + "'. Make sure that the value is either a fully qualified class name of a subclass of '"
                + PropertyNamingStrategy.class.getName()
                + "' or one of the constants defined in '" + PropertyNamingStrategies.class.getName() + "'.";
    }
}
