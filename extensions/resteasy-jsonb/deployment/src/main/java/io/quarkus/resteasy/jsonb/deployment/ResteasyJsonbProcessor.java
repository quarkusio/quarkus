package io.quarkus.resteasy.jsonb.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.bind.Jsonb;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
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
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jsonb.deployment.serializers.TypeSerializerGeneratorRegistry;
import io.quarkus.resteasy.server.common.deployment.ResteasyAdditionalReturnTypesWithoutReflectionBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerCommonProcessor;

public class ResteasyJsonbProcessor {

    private static final DotName JAX_RS_PRODUCES = DotName.createSimple("javax.ws.rs.Produces");

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

        Set<ClassType> serializerCandidates = determineSerializationCandidates(index);

        SerializerClassGenerator serializerClassGenerator = new SerializerClassGenerator(jsonbConfig);

        Map<String, String> typeToGeneratedSerializers = new HashMap<>();
        List<String> typesThatDontNeedReflection = new ArrayList<>();
        for (ClassType type : serializerCandidates) {
            SerializerClassGenerator.Result generationResult = serializerClassGenerator.generateSerializerForClassType(type,
                    typeSerializerGeneratorRegistry,
                    classOutput);
            if (generationResult.isGenerated()) {
                typeToGeneratedSerializers.put(generationResult.getClassActuallyUsed().toString(),
                        generationResult.getGeneratedClassName());
                if (!generationResult.isNeedsReflection()) {
                    typesThatDontNeedReflection.add(generationResult.getClassActuallyUsed().toString());
                }
            }
        }

        JsonbSupportClassGenerator jsonbSupportClassGenerator = new JsonbSupportClassGenerator(jsonbConfig);
        jsonbSupportClassGenerator.generateDefaultLocaleProvider(classOutput);
        jsonbSupportClassGenerator.generateJsonbDefaultJsonbDateFormatterProvider(classOutput);

        ResteasyJsonbClassGenerator resteasyJsonbClassGenerator = new ResteasyJsonbClassGenerator(jsonbConfig);
        resteasyJsonbClassGenerator.generateJsonbContextResolver(classOutput, typeToGeneratedSerializers);

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(ResteasyJsonbClassGenerator.QUARKUS_CONTEXT_RESOLVER));
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

    private Set<ClassType> determineSerializationCandidates(IndexView index) {
        Set<ClassType> serializerCandidates = new HashSet<>();
        for (DotName annotationType : ResteasyServerCommonProcessor.METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> jaxrsMethodInstances = index.getAnnotations(annotationType);
            for (AnnotationInstance jaxrsMethodInstance : jaxrsMethodInstances) {
                MethodInfo method = jaxrsMethodInstance.target().asMethod();

                if (!producesJson(method)) {
                    continue;
                }

                Type returnType = method.returnType();
                if (!ResteasyServerCommonProcessor.isReflectionDeclarationRequiredFor(returnType)
                        || returnType.name().toString().startsWith("java.lang")) {
                    continue;
                }

                if (returnType instanceof ClassType) {
                    serializerCandidates.add(returnType.asClassType());
                    continue;
                }

                // we don't generate serializers for collection types since it would override the default ones
                // we do however want to generate serializers for types that are captured by collections or Maps
                if (CollectionUtil.isCollection(returnType.name())) {
                    Type genericType = CollectionUtil.getGenericType(returnType);
                    if (genericType instanceof ClassType) {
                        serializerCandidates.add(genericType.asClassType());
                    }
                }
            }
        }
        return serializerCandidates;
    }

    private boolean producesJson(MethodInfo method) {
        AnnotationInstance produces = method.annotation(JAX_RS_PRODUCES);
        if (produces == null) {
            method.declaringClass().classAnnotation(JAX_RS_PRODUCES);
        }
        if (produces == null) {
            return false;
        }

        AnnotationValue value = produces.value();
        if (value == null) {
            return false;
        }

        for (String mediaTypeStr : value.asStringArray()) {
            MediaType mediaType = MediaType.valueOf(mediaTypeStr);
            if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
                return true;
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

}
