package io.quarkus.jackson.deployment;

import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.jackson.runtime.ObjectMapperProducer;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;

public class JacksonProcessor {

    private static final DotName JSON_DESERIALIZE = DotName.createSimple(JsonDeserialize.class.getName());

    private static final DotName JSON_SERIALIZE = DotName.createSimple(JsonSerialize.class.getName());

    private static final DotName JSON_AUTO_DETECT = DotName.createSimple(JsonAutoDetect.class.getName());

    private static final DotName JSON_CREATOR = DotName.createSimple("com.fasterxml.jackson.annotation.JsonCreator");

    private static final DotName JSON_NAMING = DotName.createSimple("com.fasterxml.jackson.databind.annotation.JsonNaming");

    private static final DotName JSON_IDENTITY_INFO = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIdentityInfo");

    private static final DotName BUILDER_VOID = DotName.createSimple(Void.class.getName());

    private static final String TIME_MODULE = "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule";

    private static final String JDK8_MODULE = "com.fasterxml.jackson.datatype.jdk8.Jdk8Module";

    private static final String PARAMETER_NAMES_MODULE = "com.fasterxml.jackson.module.paramnames.ParameterNamesModule";

    // this list can probably be enriched with more modules
    private static final List<String> MODULES_NAMES_TO_AUTO_REGISTER = Arrays.asList(TIME_MODULE, JDK8_MODULE,
            PARAMETER_NAMES_MODULE);

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    List<IgnoreJsonDeserializeClassBuildItem> ignoreJsonDeserializeClassBuildItems;

    @BuildStep
    void unremovable(Capabilities capabilities, BuildProducer<UnremovableBeanBuildItem> producer) {
        if (capabilities.isPresent(Capability.VERTX_CORE)) {
            producer.produce(UnremovableBeanBuildItem.beanTypes(ObjectMapper.class));
        }
    }

    @BuildStep
    void register(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(true, false, "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                        "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer",
                        "com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer",
                        "com.fasterxml.jackson.annotation.SimpleObjectIdResolver"));

        IndexView index = combinedIndexBuildItem.getIndex();

        // TODO: @JsonDeserialize is only supported as a class annotation - we should support the others as well

        Set<DotName> ignoredDotNames = new HashSet<>();
        for (IgnoreJsonDeserializeClassBuildItem ignoreJsonDeserializeClassBuildItem : ignoreJsonDeserializeClassBuildItems) {
            ignoredDotNames.add(ignoreJsonDeserializeClassBuildItem.getDotName());
        }

        // handle the various @JsonDeserialize cases
        for (AnnotationInstance deserializeInstance : index.getAnnotations(JSON_DESERIALIZE)) {
            AnnotationTarget annotationTarget = deserializeInstance.target();
            if (CLASS.equals(annotationTarget.kind())) {
                DotName dotName = annotationTarget.asClass().name();
                if (!ignoredDotNames.contains(dotName)) {
                    addReflectiveHierarchyClass(dotName, reflectiveHierarchyClass);
                }

                AnnotationValue annotationValue = deserializeInstance.value("builder");
                if (null != annotationValue && AnnotationValue.Kind.CLASS.equals(annotationValue.kind())) {
                    DotName builderClassName = annotationValue.asClass().name();
                    if (!BUILDER_VOID.equals(builderClassName)) {
                        addReflectiveHierarchyClass(builderClassName, reflectiveHierarchyClass);
                    }
                }
            }
            AnnotationValue usingValue = deserializeInstance.value("using");
            if (usingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, usingValue.asClass().name().toString()));
            }
            AnnotationValue keyUsingValue = deserializeInstance.value("keyUsing");
            if (keyUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, keyUsingValue.asClass().name().toString()));
            }
            AnnotationValue contentUsingValue = deserializeInstance.value("contentUsing");
            if (contentUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(false, false, contentUsingValue.asClass().name().toString()));
            }
        }

        // handle the various @JsonSerialize cases
        for (AnnotationInstance serializeInstance : index.getAnnotations(JSON_SERIALIZE)) {
            AnnotationValue usingValue = serializeInstance.value("using");
            if (usingValue != null) {
                // the Serializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, usingValue.asClass().name().toString()));
            }
            AnnotationValue keyUsingValue = serializeInstance.value("keyUsing");
            if (keyUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, keyUsingValue.asClass().name().toString()));
            }
            AnnotationValue contentUsingValue = serializeInstance.value("contentUsing");
            if (contentUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(false, false, contentUsingValue.asClass().name().toString()));
            }
            AnnotationValue nullsUsingValue = serializeInstance.value("nullsUsing");
            if (nullsUsingValue != null) {
                // the Deserializers are constructed internally by Jackson using a no-args constructor
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(false, false, nullsUsingValue.asClass().name().toString()));
            }
        }

        for (AnnotationInstance creatorInstance : index.getAnnotations(JSON_AUTO_DETECT)) {
            if (creatorInstance.target().kind().equals(CLASS)) {
                reflectiveClass
                        .produce(
                                new ReflectiveClassBuildItem(true, true, creatorInstance.target().asClass().name().toString()));
            }
        }

        // make sure we register the constructors and methods marked with @JsonCreator for reflection
        for (AnnotationInstance creatorInstance : index.getAnnotations(JSON_CREATOR)) {
            if (METHOD == creatorInstance.target().kind()) {
                reflectiveMethod.produce(new ReflectiveMethodBuildItem(creatorInstance.target().asMethod()));
            }
        }

        // register @JsonNaming strategy implementations for reflection
        for (AnnotationInstance jsonNamingInstance : index.getAnnotations(JSON_NAMING)) {
            AnnotationValue strategyValue = jsonNamingInstance.value("value");
            if (strategyValue != null) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, strategyValue.asClass().name().toString()));
            }
        }

        // register @JsonIdentityInfo strategy implementations for reflection
        for (AnnotationInstance jsonIdentityInfoInstance : index.getAnnotations(JSON_IDENTITY_INFO)) {
            AnnotationValue generatorValue = jsonIdentityInfoInstance.value("generator");
            AnnotationValue resolverValue = jsonIdentityInfoInstance.value("resolver");
            if (generatorValue != null) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, generatorValue.asClass().name().toString()));
            }
            if (resolverValue != null) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, resolverValue.asClass().name().toString()));
            } else {
                // Registering since SimpleObjectIdResolver is the default value of @JsonIdentityInfo.resolver
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, SimpleObjectIdResolver.class));
            }
        }

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBeans.produce(new AdditionalBeanBuildItem(ObjectMapperProducer.class));
    }

    private void addReflectiveHierarchyClass(DotName className,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass) {
        Type jandexType = Type.create(className, Type.Kind.CLASS);
        reflectiveHierarchyClass.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(jandexType)
                .source(getClass().getSimpleName() + " > " + jandexType.name().toString())
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
        try {
            Class.forName(moduleClassName, false, Thread.currentThread().getContextClassLoader());
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(moduleClassName));
        } catch (Exception ignored) {
        }
    }

    // Generate a ObjectMapperCustomizer bean that registers each serializer / deserializer as well as detected modules with the ObjectMapper
    @BuildStep
    void generateCustomizer(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            List<JacksonModuleBuildItem> jacksonModules, List<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {

        if (jacksonModules.isEmpty() && classPathJacksonModules.isEmpty()) {
            return;
        }

        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className("io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer")
                .interfaces(ObjectMapperCustomizer.class.getName())
                .build()) {
            classCreator.addAnnotation(Singleton.class);

            try (MethodCreator customize = classCreator.getMethodCreator("customize", void.class, ObjectMapper.class)) {
                ResultHandle objectMapper = customize.getMethodParam(0);

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
                    ResultHandle module = customize.newInstance(
                            MethodDescriptor.ofConstructor(SimpleModule.class, String.class),
                            customize.load(jacksonModule.getName()));

                    for (JacksonModuleBuildItem.Item item : jacksonModule.getItems()) {
                        ResultHandle targetClass = customize.loadClass(item.getTargetClassName());

                        String serializerClassName = item.getSerializerClassName();
                        if (serializerClassName != null && !serializerClassName.isEmpty()) {
                            ResultHandle serializer = customize.newInstance(
                                    MethodDescriptor.ofConstructor(serializerClassName));
                            customize.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(SimpleModule.class, "addSerializer", SimpleModule.class,
                                            Class.class, JsonSerializer.class),
                                    module, targetClass, serializer);
                        }

                        String deserializerClassName = item.getDeserializerClassName();
                        if (deserializerClassName != null && !deserializerClassName.isEmpty()) {
                            ResultHandle deserializer = customize.newInstance(
                                    MethodDescriptor.ofConstructor(deserializerClassName));
                            customize.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(SimpleModule.class, "addDeserializer", SimpleModule.class,
                                            Class.class, JsonDeserializer.class),
                                    module, targetClass, deserializer);
                        }
                    }

                    customize.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ObjectMapper.class, "registerModule", ObjectMapper.class, Module.class),
                            objectMapper, module);
                }

                for (ClassPathJacksonModuleBuildItem classPathJacksonModule : classPathJacksonModules) {
                    ResultHandle module = customize
                            .newInstance(MethodDescriptor.ofConstructor(classPathJacksonModule.getModuleClassName()));
                    customize.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ObjectMapper.class, "registerModule", ObjectMapper.class, Module.class),
                            objectMapper, module);
                }

                customize.returnValue(null);
            }

            // ensure that the things we auto-register have the lower priority - this ensures that user registered modules take priority
            try (MethodCreator priority = classCreator.getMethodCreator("priority", int.class)) {
                priority.returnValue(priority.load(ObjectMapperCustomizer.QUARKUS_CUSTOMIZER_PRIORITY));
            }
        }
    }
}
