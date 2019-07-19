package io.quarkus.resteasy.jsonb.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.bind.Jsonb;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.ext.Provider;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jsonb.deployment.serializers.GlobalSerializationConfig;
import io.quarkus.resteasy.jsonb.deployment.serializers.TypeSerializerGenerator;
import io.quarkus.resteasy.jsonb.deployment.serializers.TypeSerializerGeneratorRegistry;
import io.quarkus.resteasy.server.common.deployment.ResteasyAdditionalReturnTypesWithoutReflectionBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerCommonProcessor;

public class ResteasyJsonbProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JSONB));
    }

    JsonbConfig jsonbConfig;

    /*
     * If possible we are going to create a serializer for the class
     * indicated by returnType
     * We only create serializers for types we are 100% sure we can handle
     * Whenever we encounter something we can't handle,
     * we don't create a serializer and therefore fallback to
     * jsonb to do it's runtime reflection work
     */
    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider,
            BuildProducer<ResteasyAdditionalReturnTypesWithoutReflectionBuildItem> typesWithoutReflection) {
        IndexView index = combinedIndexBuildItem.getIndex();

        if (!jsonbConfig.enabled) {
            return;
        }

        // if the user has declared a custom ContextResolver for Jsonb, we don't generate anything
        if (hasCustomContextResolverBeenDeclared(index)) {
            return;
        }

        validateConfiguration();

        SerializationClassInspector serializationClassInspector = new SerializationClassInspector(index);
        TypeSerializerGeneratorRegistry typeSerializerGeneratorRegistry = new TypeSerializerGeneratorRegistry(
                serializationClassInspector);

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };

        Set<ClassType> serializerCandidates = new HashSet<>();
        for (DotName annotationType : ResteasyServerCommonProcessor.METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> jaxrsMethodInstances = index.getAnnotations(annotationType);
            for (AnnotationInstance jaxrsMethodInstance : jaxrsMethodInstances) {
                MethodInfo method = jaxrsMethodInstance.target().asMethod();
                Type returnType = method.returnType();
                if (!ResteasyServerCommonProcessor.isReflectionDeclarationRequiredFor(returnType)
                        || returnType.name().toString().startsWith("java.lang")) {
                    continue;
                }

                if (returnType instanceof ClassType) {
                    serializerCandidates.add(returnType.asClassType());
                    continue;
                }

                // don't generate serializers for collection types since it would override the default ones
                // we do however want to generate serializers for types that are captured by collections or Maps
                if (CollectionUtil.isCollection(returnType.name())) {
                    Type genericType = CollectionUtil.getGenericType(returnType);
                    if (genericType instanceof ClassType) {
                        serializerCandidates.add(genericType.asClassType());
                    }
                }
            }
        }

        Map<String, String> typeToGeneratedSerializers = new HashMap<>();
        List<String> typesThatDontNeedReflection = new ArrayList<>();
        for (ClassType type : serializerCandidates) {
            GenerationResult generationResult = generateSerializerForClassType(type,
                    typeSerializerGeneratorRegistry,
                    classOutput);
            if (generationResult.isGenerated()) {
                typeToGeneratedSerializers.put(type.name().toString(), generationResult.getClassName());
                if (!generationResult.isNeedsReflection()) {
                    typesThatDontNeedReflection.add(type.name().toString());
                }
            }
        }

        AdditionalClassGenerator additionalClassGenerator = new AdditionalClassGenerator(jsonbConfig);
        additionalClassGenerator.generateDefaultLocaleProvider(classOutput);
        additionalClassGenerator.generateJsonbDefaultJsonbDateFormatterProvider(classOutput);
        additionalClassGenerator.generateJsonbContextResolver(classOutput, typeToGeneratedSerializers);

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(AdditionalClassGenerator.QUARKUS_CONTEXT_RESOLVER));
        for (String type : typesThatDontNeedReflection) {
            typesWithoutReflection.produce(new ResteasyAdditionalReturnTypesWithoutReflectionBuildItem(type));
        }
    }

    private boolean hasCustomContextResolverBeenDeclared(IndexView index) {
        for (ClassInfo contextResolver : index.getAllKnownImplementors(DotNames.CONTEXT_RESOLVER)) {
            if (contextResolver.classAnnotation(DotName.createSimple(Provider.class.getName())) == null) {
                continue;
            }

            for (Type interfacesType : contextResolver.interfaceTypes()) {
                if (!DotNames.CONTEXT_RESOLVER.equals(interfacesType.name())) {
                    continue;
                }

                // make sure we are only dealing with implementations that have set the generic type of ContextResolver
                if (!(interfacesType instanceof ParameterizedType)) {
                    continue;
                }

                List<Type> contextResolverGenericArguments = interfacesType.asParameterizedType().arguments();
                if (contextResolverGenericArguments.size() != 1) {
                    continue; // shouldn't ever happen
                }

                Type firstGenericType = contextResolverGenericArguments.get(0);
                if ((firstGenericType instanceof ClassType) &&
                        firstGenericType.asClassType().name().equals(DotName.createSimple(Jsonb.class.getName()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateConfiguration() {
        if (!jsonbConfig.isValidPropertyOrderStrategy()) {
            throw new IllegalArgumentException(
                    "quarkus.jsonb.property-order-strategy can only be one of " + JsonbConfig.ALLOWED_PROPERTY_ORDER_VALUES);
        }
    }

    /**
     * @return The full name of the generated class or null if a serializer could not be generated
     */
    private GenerationResult generateSerializerForClassType(ClassType classType, TypeSerializerGeneratorRegistry registry,
            ClassOutput classOutput) {
        TypeSerializerGenerator.Supported supported = registry.getObjectSerializer().supports(classType, registry);
        if (supported == TypeSerializerGenerator.Supported.UNSUPPORTED) {
            return GenerationResult.notGenerated();
        }

        DotName classDotName = classType.name();
        String generatedSerializerName = "io.quarkus.jsonb.serializers." + classDotName.withoutPackagePrefix() + "Serializer";
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedSerializerName)
                .interfaces(JsonbSerializer.class)
                .signature(String.format("Ljava/lang/Object;Ljavax/json/bind/serializer/JsonbSerializer<L%s;>;",
                        classDotName.toString()).replace('.', '/'))
                .build()) {

            // actual implementation of serialize method
            try (MethodCreator serialize = cc.getMethodCreator("serialize", "void", classDotName.toString(),
                    JsonGenerator.class.getName(), SerializationContext.class.getName())) {
                ResultHandle object = serialize.getMethodParam(0);
                ResultHandle jsonGenerator = serialize.getMethodParam(1);
                ResultHandle serializationContext = serialize.getMethodParam(2);

                // delegate to object serializer
                registry.getObjectSerializer().generate(
                        new TypeSerializerGenerator.GenerateContext(classType, serialize, jsonGenerator, serializationContext,
                                object, registry,
                                getGlobalConfig(), false, null));

                serialize.returnValue(null);
            }

            // bridge method
            try (MethodCreator bridgeSerialize = cc.getMethodCreator("serialize", "void", Object.class, JsonGenerator.class,
                    SerializationContext.class)) {
                MethodDescriptor serialize = MethodDescriptor.ofMethod(generatedSerializerName, "serialize", "void",
                        classDotName.toString(),
                        JsonGenerator.class.getName(), SerializationContext.class.getName());
                ResultHandle castedObject = bridgeSerialize.checkCast(bridgeSerialize.getMethodParam(0),
                        classDotName.toString());
                bridgeSerialize.invokeVirtualMethod(serialize, bridgeSerialize.getThis(),
                        castedObject, bridgeSerialize.getMethodParam(1), bridgeSerialize.getMethodParam(2));
                bridgeSerialize.returnValue(null);
            }
        }

        return supported == TypeSerializerGenerator.Supported.FULLY
                ? GenerationResult.noReflectionNeeded(generatedSerializerName)
                : GenerationResult.reflectionNeeded(generatedSerializerName);
    }

    private GlobalSerializationConfig getGlobalConfig() {
        return new GlobalSerializationConfig(
                jsonbConfig.locale, jsonbConfig.dateFormat, jsonbConfig.serializeNullValues, jsonbConfig.propertyOrderStrategy);
    }

    static class GenerationResult {
        private final boolean generated;
        private final String className;
        private final boolean needsReflection;

        private GenerationResult(boolean generated, String className, boolean needsReflection) {
            this.generated = generated;
            this.className = className;
            this.needsReflection = needsReflection;
        }

        static GenerationResult notGenerated() {
            return new GenerationResult(false, null, false);
        }

        static GenerationResult noReflectionNeeded(String className) {
            return new GenerationResult(true, className, false);
        }

        static GenerationResult reflectionNeeded(String className) {
            return new GenerationResult(true, className, true);
        }

        boolean isGenerated() {
            return generated;
        }

        String getClassName() {
            return className;
        }

        boolean isNeedsReflection() {
            return needsReflection;
        }
    }

}
