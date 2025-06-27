package io.quarkus.jsonb.deployment;

import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.beans.ConstructorProperties;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.spi.JsonbProvider;

import org.eclipse.yasson.spi.JsonbComponentInstanceCreator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jsonb.JsonbConfigCustomizer;
import io.quarkus.jsonb.JsonbProducer;
import io.quarkus.jsonb.QuarkusJsonbComponentInstanceCreator;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;

public class JsonbProcessor {

    static final DotName JSONB_ADAPTER_NAME = DotName.createSimple(JsonbAdapter.class.getName());

    private static final DotName JSONB_TYPE_SERIALIZER = DotName.createSimple(JsonbTypeSerializer.class.getName());
    private static final DotName JSONB_TYPE_DESERIALIZER = DotName.createSimple(JsonbTypeDeserializer.class.getName());

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        resourceBundle.produce(new NativeImageResourceBundleBuildItem("yasson-messages"));

        serviceProvider.produce(new ServiceProviderBuildItem(JsonbComponentInstanceCreator.class.getName(),
                QuarkusJsonbComponentInstanceCreator.class.getName()));

        // Accessed in jakarta.json.bind.spi.JsonbProvider.provider()
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(JsonbProvider.class.getName()));

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBeans.produce(new AdditionalBeanBuildItem(JsonbProducer.class));

        IndexView index = combinedIndexBuildItem.getIndex();

        // handle the various @JsonSerialize cases
        for (AnnotationInstance serializeInstance : index.getAnnotations(JSONB_TYPE_SERIALIZER)) {
            registerInstance(reflectiveClass, serializeInstance);
        }

        // handle the various @JsonDeserialize cases
        for (AnnotationInstance deserializeInstance : index.getAnnotations(JSONB_TYPE_DESERIALIZER)) {
            registerInstance(reflectiveClass, deserializeInstance);
        }

        // register String constructors for reflection as they may not have been properly registered by default
        // see https://github.com/quarkusio/quarkus/issues/10873
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("java.lang.String").build());

        // register `java.beans.ConstructorProperties` as it's accessed through `io.quarkus.jsonb.JsonbProducer.jsonb`
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(ConstructorProperties.class).build());

        // Necessary for Yasson versions using MethodHandles (2.0+)
        reflectiveMethod.produce(new ReflectiveMethodBuildItem(
                getClass().getName(),
                "jdk.internal.misc.Unsafe", "putReference", Object.class,
                long.class, Object.class));
    }

    private void registerInstance(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, AnnotationInstance instance) {
        AnnotationTarget annotationTarget = instance.target();
        if (FIELD.equals(annotationTarget.kind()) || METHOD.equals(annotationTarget.kind())) {
            AnnotationValue value = instance.value();
            if (value != null) {
                // the Deserializers are constructed internally by JSON-B using a no-args constructor
                reflectiveClass.produce(
                        ReflectiveClassBuildItem.builder(value.asClass().name().toString()).build());
            }
        }
    }

    @BuildStep
    void processJsonbAdapters(BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<AutoAddScopeBuildItem> autoScopes) {

        // An adapter with an injection point but no scope is @Singleton
        autoScopes.produce(AutoAddScopeBuildItem.builder().implementsInterface(JSONB_ADAPTER_NAME).requiresContainerServices()
                .defaultScope(BuiltinScope.SINGLETON).build());

        // Make all adapters unremovable
        unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {

            @Override
            public boolean test(BeanInfo bean) {
                return bean.isClassBean() && bean.hasType(JSONB_ADAPTER_NAME);
            }
        }));
    }

    // Generate a JsonbConfigCustomizer bean that registers each serializer / deserializer with JsonbConfig
    @BuildStep
    void generateCustomizer(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            List<JsonbSerializerBuildItem> serializers,
            List<JsonbDeserializerBuildItem> deserializers) {

        if (serializers.isEmpty()) {
            return;
        }

        final Set<String> customSerializerClasses = new HashSet<>();
        final Set<String> customDeserializerClasses = new HashSet<>();
        for (JsonbSerializerBuildItem serializer : serializers) {
            customSerializerClasses.addAll(serializer.getSerializerClassNames());
        }
        for (JsonbDeserializerBuildItem deserializer : deserializers) {
            customDeserializerClasses.addAll(deserializer.getDeserializerClassNames());
        }
        if (customSerializerClasses.isEmpty() && customDeserializerClasses.isEmpty()) {
            return;
        }

        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className("io.quarkus.jsonb.customizer.RegisterSerializersAndDeserializersCustomizer")
                .interfaces(JsonbConfigCustomizer.class.getName())
                .build()) {
            classCreator.addAnnotation(Singleton.class);

            try (MethodCreator customize = classCreator.getMethodCreator("customize", void.class, JsonbConfig.class)) {
                ResultHandle jsonbConfig = customize.getMethodParam(0);
                if (!customSerializerClasses.isEmpty()) {
                    ResultHandle serializersArray = customize.newArray(JsonbSerializer.class, customSerializerClasses.size());
                    int i = 0;
                    for (String customSerializerClass : customSerializerClasses) {
                        customize.writeArrayValue(serializersArray, i,
                                customize.newInstance(MethodDescriptor.ofConstructor(customSerializerClass)));
                        i++;
                    }
                    customize.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(JsonbConfig.class, "withSerializers", JsonbConfig.class,
                                    JsonbSerializer[].class),
                            jsonbConfig, serializersArray);
                }
                if (!customDeserializerClasses.isEmpty()) {
                    ResultHandle deserializersArray = customize.newArray(JsonbDeserializer.class,
                            customDeserializerClasses.size());
                    int i = 0;
                    for (String customDeserializerClass : customDeserializerClasses) {
                        customize.writeArrayValue(deserializersArray, i,
                                customize.newInstance(MethodDescriptor.ofConstructor(customDeserializerClass)));
                        i++;
                    }
                    customize.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(JsonbConfig.class, "withDeserializers", JsonbConfig.class,
                                    JsonbDeserializer[].class),
                            jsonbConfig, deserializersArray);
                }

                customize.returnValue(null);
            }
        }
    }
}
