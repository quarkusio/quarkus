package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.JSON_IGNORE;
import static io.quarkus.security.spi.RolesAllowedConfigExpResolverBuildItem.isSecurityConfigExpressionCandidate;
import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_NDJSON;
import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_STREAM_JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.inject.Singleton;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.util.MethodId;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.JaxRsResourceIndexBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.EnableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.NativeInvalidDefinitionExceptionMapper;
import io.quarkus.resteasy.reactive.jackson.runtime.security.RolesAllowedConfigExpStorage;
import io.quarkus.resteasy.reactive.jackson.runtime.security.SecurityCustomSerialization;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.*;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyWriter;
import io.quarkus.resteasy.reactive.server.deployment.ContextResolversBuildItem;
import io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveResourceMethodEntriesBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.security.spi.RolesAllowedConfigExpResolverBuildItem;
import io.quarkus.vertx.deployment.ReinitializeVertxJsonBuildItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResteasyReactiveJacksonProcessor {

    private static final Logger log = Logger.getLogger(ResteasyReactiveJacksonProcessor.class);

    private static final DotName JSON_VIEW = DotName.createSimple(JsonView.class.getName());
    private static final DotName CUSTOM_SERIALIZATION = DotName.createSimple(CustomSerialization.class.getName());
    private static final DotName CUSTOM_DESERIALIZATION = DotName.createSimple(CustomDeserialization.class.getName());
    private static final DotName SECURE_FIELD = DotName.createSimple(SecureField.class.getName());
    private static final DotName DISABLE_SECURE_SERIALIZATION = DotName
            .createSimple(DisableSecureSerialization.class.getName());
    private static final DotName ENABLE_SECURE_SERIALIZATION = DotName
            .createSimple(EnableSecureSerialization.class.getName());

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final List<String> HANDLED_MEDIA_TYPES = List.of(MediaType.APPLICATION_JSON, APPLICATION_NDJSON,
            APPLICATION_STREAM_JSON);
    public static final String DEFAULT_MISMATCHED_INPUT_EXCEPTION = "io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.REST_JACKSON));
    }

    @BuildStep
    ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return ServerDefaultProducesHandlerBuildItem.json();
    }

    @BuildStep
    ResteasyReactiveJacksonProviderDefinedBuildItem jacksonRegistered() {
        return new ResteasyReactiveJacksonProviderDefinedBuildItem();
    }

    @BuildStep
    ReinitializeVertxJsonBuildItem vertxJson() {
        return new ReinitializeVertxJsonBuildItem();
    }

    @BuildStep
    void exceptionMappers(BuildProducer<ExceptionMapperBuildItem> producer) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(DEFAULT_MISMATCHED_INPUT_EXCEPTION);
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            // the class is not available, likely due to quarkus.class-loading.removed-resources."io.quarkus\:quarkus-rest-jackson"=io/quarkus/resteasy/reactive/jackson/runtime/mappers/DefaultMismatchedInputException.class
            return;
        }

        producer.produce(new ExceptionMapperBuildItem(DEFAULT_MISMATCHED_INPUT_EXCEPTION,
                MismatchedInputException.class.getName(), Priorities.USER + 100, false));
    }

    @BuildStep
    CustomExceptionMapperBuildItem customExceptionMappers() {
        return new CustomExceptionMapperBuildItem(NativeInvalidDefinitionExceptionMapper.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // make these beans to they can get instantiated with the Quarkus CDI configured ObjectMapper object
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(ServerJacksonMessageBodyReader.class.getName())
                .addBeanClass(FullyFeaturedServerJacksonMessageBodyReader.class)
                .addBeanClass(BasicServerJacksonMessageBodyWriter.class)
                // This will not be needed in most cases, but it will not be involved in serialization
                // just because it's a bean.
                // Whether it is used in RESTEasy Reactive is determined elsewhere
                .addBeanClass(FullyFeaturedServerJacksonMessageBodyWriter.class)
                .setUnremovable().build();
    }

    @BuildStep
    void additionalProviders(ContextResolversBuildItem contextResolversBuildItem,
            List<JacksonFeatureBuildItem> jacksonFeatureBuildItems,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        boolean specialJacksonFeaturesUsed = !jacksonFeatureBuildItems.isEmpty();
        boolean hasObjectMapperContextResolver = contextResolversBuildItem.getContextResolvers().getResolvers()
                .containsKey(ObjectMapper.class);

        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(
                                getJacksonMessageBodyReader(
                                        hasObjectMapperContextResolver || specialJacksonFeaturesUsed),
                                Object.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true).setRuntimeType(RuntimeType.SERVER).build());
        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(VertxJsonArrayMessageBodyReader.class.getName(),
                                JsonArray.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true)
                                .setRuntimeType(RuntimeType.SERVER)
                                .build());
        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(VertxJsonObjectMessageBodyReader.class.getName(),
                                JsonObject.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true)
                                .setRuntimeType(RuntimeType.SERVER)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(
                                getJacksonMessageBodyWriter(
                                        hasObjectMapperContextResolver || specialJacksonFeaturesUsed),
                                Object.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true)
                                .setRuntimeType(RuntimeType.SERVER)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(VertxJsonArrayMessageBodyWriter.class.getName(),
                                JsonArray.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true)
                                .setRuntimeType(RuntimeType.SERVER)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(VertxJsonObjectMessageBodyWriter.class.getName(),
                                JsonObject.class.getName())
                                .setMediaTypeStrings(HANDLED_MEDIA_TYPES)
                                .setBuiltin(true)
                                .setRuntimeType(RuntimeType.SERVER)
                                .build());
    }

    private String getJacksonMessageBodyWriter(boolean needsFullFeatureSet) {
        return needsFullFeatureSet ? FullyFeaturedServerJacksonMessageBodyWriter.class.getName()
                : BasicServerJacksonMessageBodyWriter.class.getName();
    }

    private String getJacksonMessageBodyReader(boolean needsFullFeatureSet) {
        return needsFullFeatureSet ? FullyFeaturedServerJacksonMessageBodyReader.class.getName()
                : ServerJacksonMessageBodyReader.class.getName();
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(ReflectiveClassBuildItem.builder(Cookie.class).reason(getClass().getName()).methods().build());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void handleJsonAnnotations(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            CombinedIndexBuildItem index,
            List<ResourceMethodCustomSerializationBuildItem> resourceMethodCustomSerializationBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<JacksonFeatureBuildItem> jacksonFeaturesProducer,
            ResteasyReactiveServerJacksonRecorder recorder, ShutdownContextBuildItem shutdown) {
        if (resourceScanningResultBuildItem.isEmpty()) {
            return;
        }
        Collection<ClassInfo> resourceClasses = resourceScanningResultBuildItem.get().getResult().getScannedResources()
                .values();
        Set<JacksonFeatureBuildItem.Feature> jacksonFeatures = new HashSet<>();
        for (ClassInfo resourceClass : resourceClasses) {
            if (resourceClass.annotationsMap().containsKey(JSON_VIEW)) {
                jacksonFeatures.add(JacksonFeatureBuildItem.Feature.JSON_VIEW);
                for (AnnotationInstance instance : resourceClass.annotationsMap().get(JSON_VIEW)) {
                    AnnotationValue annotationValue = instance.value();
                    if (annotationValue == null) {
                        continue;
                    }
                    Type[] jsonViews = annotationValue.asClassArray();
                    if ((jsonViews == null) || (jsonViews.length == 0)) {
                        continue;
                    }
                    recorder.recordJsonView(getTargetId(instance.target()), jsonViews[0].name().toString());
                }
            }
            if (resourceClass.annotationsMap().containsKey(CUSTOM_SERIALIZATION)) {
                jacksonFeatures.add(JacksonFeatureBuildItem.Feature.CUSTOM_SERIALIZATION);
                for (AnnotationInstance instance : resourceClass.annotationsMap().get(CUSTOM_SERIALIZATION)) {
                    AnnotationValue annotationValue = instance.value();
                    if (annotationValue == null) {
                        continue;
                    }
                    Type biFunctionType = annotationValue.asClass();
                    if (biFunctionType == null) {
                        continue;
                    }
                    ClassInfo biFunctionClassInfo = index.getIndex().getClassByName(biFunctionType.name());
                    if (biFunctionClassInfo == null) {
                        // be lenient
                    } else {
                        if (!biFunctionClassInfo.hasNoArgsConstructor()) {
                            throw new IllegalArgumentException(
                                    "Class '" + biFunctionClassInfo.name() + "' must contain a no-args constructor");
                        }
                    }
                    reflectiveClassProducer.produce(
                            ReflectiveClassBuildItem.builder(biFunctionType.name().toString())
                                    .reason(getClass().getName())
                                    .build());
                    recorder.recordCustomSerialization(getTargetId(instance.target()), biFunctionType.name().toString());
                }
            }
            if (resourceClass.annotationsMap().containsKey(CUSTOM_DESERIALIZATION)) {
                jacksonFeatures.add(JacksonFeatureBuildItem.Feature.CUSTOM_DESERIALIZATION);
                for (AnnotationInstance instance : resourceClass.annotationsMap().get(CUSTOM_DESERIALIZATION)) {
                    AnnotationValue annotationValue = instance.value();
                    if (annotationValue == null) {
                        continue;
                    }
                    Type biFunctionType = annotationValue.asClass();
                    if (biFunctionType == null) {
                        continue;
                    }
                    ClassInfo biFunctionClassInfo = index.getIndex().getClassByName(biFunctionType.name());
                    if (biFunctionClassInfo == null) {
                        // be lenient
                    } else {
                        if (!biFunctionClassInfo.hasNoArgsConstructor()) {
                            throw new IllegalArgumentException(
                                    "Class '" + biFunctionClassInfo.name() + "' must contain a no-args constructor");
                        }
                    }
                    reflectiveClassProducer.produce(
                            ReflectiveClassBuildItem.builder(biFunctionType.name().toString())
                                    .reason(getClass().getName())
                                    .build());
                    recorder.recordCustomDeserialization(getTargetId(instance.target()), biFunctionType.name().toString());
                }
            }
        }

        for (ResourceMethodCustomSerializationBuildItem bi : resourceMethodCustomSerializationBuildItems) {
            jacksonFeatures.add(JacksonFeatureBuildItem.Feature.CUSTOM_SERIALIZATION);
            String className = bi.getCustomSerializationProvider().getName();
            reflectiveClassProducer.produce(
                    ReflectiveClassBuildItem.builder(className)
                            .reason(getClass().getName())
                            .build());
            recorder.recordCustomSerialization(getMethodId(bi.getMethodInfo(), bi.getDeclaringClassInfo()), className);
        }

        if (!jacksonFeatures.isEmpty()) {
            for (JacksonFeatureBuildItem.Feature jacksonFeature : jacksonFeatures) {
                jacksonFeaturesProducer.produce(new JacksonFeatureBuildItem(jacksonFeature));
            }
            recorder.configureShutdown(shutdown);
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void resolveRolesAllowedConfigExpressions(BuildProducer<RolesAllowedConfigExpResolverBuildItem> resolverProducer,
            Capabilities capabilities, ResteasyReactiveServerJacksonRecorder recorder, CombinedIndexBuildItem indexBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            BuildProducer<InitAndValidateRolesAllowedConfigExp> initAndValidateItemProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            BiConsumer<String, Supplier<String[]>> configValRecorder = null;
            for (AnnotationInstance instance : indexBuildItem.getIndex().getAnnotations(SECURE_FIELD)) {
                for (String role : instance.value("rolesAllowed").asStringArray()) {
                    if (isSecurityConfigExpressionCandidate(role)) {
                        if (configValRecorder == null) {
                            var storage = recorder.createConfigExpToAllowedRoles();
                            configValRecorder = recorder.recordRolesAllowedConfigExpression(storage);
                            syntheticBeanProducer.produce(SyntheticBeanBuildItem
                                    .configure(RolesAllowedConfigExpStorage.class)
                                    .scope(Singleton.class)
                                    .supplier(recorder.createRolesAllowedConfigExpStorage(storage))
                                    .unremovable()
                                    .done());
                            initAndValidateItemProducer.produce(new InitAndValidateRolesAllowedConfigExp());
                        }
                        resolverProducer.produce(new RolesAllowedConfigExpResolverBuildItem(role, configValRecorder));
                    }
                }
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @Consume(SynthesisFinishedBuildItem.class)
    public void initializeRolesAllowedConfigExp(ResteasyReactiveServerJacksonRecorder recorder,
            Optional<InitAndValidateRolesAllowedConfigExp> initAndValidateItem) {
        if (initAndValidateItem.isPresent()) {
            recorder.initAndValidateRolesAllowedConfigExp();
        }
    }

    @BuildStep(onlyIf = JacksonOptimizationConfig.IsReflectionFreeSerializersEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void handleEndpointParams(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntries,
            JaxRsResourceIndexBuildItem jaxRsIndex, CombinedIndexBuildItem index,
            ResteasyReactiveServerJacksonRecorder recorder,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {

        IndexView indexView = jaxRsIndex.getIndexView();

        Map<String, ClassInfo> serializedClasses = new HashMap<>();
        Map<String, ClassInfo> deserializedClasses = new HashMap<>();

        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : resourceMethodEntries.getEntries()) {
            MethodInfo methodInfo = entry.getMethodInfo();
            ClassInfo effectiveReturnClassInfo = getEffectiveClassInfo(methodInfo.returnType(), indexView);
            if (effectiveReturnClassInfo != null) {
                serializedClasses.put(effectiveReturnClassInfo.name().toString(), effectiveReturnClassInfo);
            }

            if (methodInfo.hasAnnotation(POST.class)) {
                for (Type paramType : methodInfo.parameterTypes()) {
                    ClassInfo effectiveParamClassInfo = getEffectiveClassInfo(paramType, indexView);
                    if (effectiveParamClassInfo != null) {
                        deserializedClasses.put(effectiveParamClassInfo.name().toString(), effectiveParamClassInfo);
                    }
                }
            }
        }

        if (!serializedClasses.isEmpty()) {
            JacksonSerializerFactory factory = new JacksonSerializerFactory(generatedClassBuildItemBuildProducer,
                    index.getComputingIndex());
            factory.create(serializedClasses.values())
                    .forEach(recorder::recordGeneratedSerializer);
        }

        if (!deserializedClasses.isEmpty()) {
            JacksonDeserializerFactory factory = new JacksonDeserializerFactory(generatedClassBuildItemBuildProducer,
                    index.getComputingIndex());
            factory.create(deserializedClasses.values())
                    .forEach(recorder::recordGeneratedDeserializer);
        }
    }

    @BuildStep(onlyIf = JacksonOptimizationConfig.IsReflectionFreeSerializersEnabled.class)
    void unremovable(BuildProducer<AdditionalBeanBuildItem> additionalProducer) {
        additionalProducer.produce(AdditionalBeanBuildItem.unremovableOf(GeneratedSerializersRegister.class));
    }

    @BuildStep
    public void handleFieldSecurity(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntries,
            JaxRsResourceIndexBuildItem index,
            BuildProducer<ResourceMethodCustomSerializationBuildItem> producer) {
        IndexView indexView = index.getIndexView();
        boolean noSecureFieldDetected = indexView.getAnnotations(SECURE_FIELD).isEmpty();
        if (noSecureFieldDetected) {
            return;
        }

        Map<String, Boolean> typeToHasSecureField = new HashMap<>(getTypesWithSecureField());
        List<ResourceMethodCustomSerializationBuildItem> result = new ArrayList<>();
        for (ResteasyReactiveResourceMethodEntriesBuildItem.Entry entry : resourceMethodEntries.getEntries()) {
            MethodInfo methodInfo = entry.getMethodInfo();
            if (methodInfo.hasAnnotation(DISABLE_SECURE_SERIALIZATION)) {
                continue;
            }
            if (entry.getActualClassInfo().declaredAnnotation(DISABLE_SECURE_SERIALIZATION) != null) {
                if (!methodInfo.hasAnnotation(ENABLE_SECURE_SERIALIZATION)) {
                    continue;
                }
            }

            ResourceMethod resourceInfo = entry.getResourceMethod();
            boolean isJsonResponse = false;
            if (resourceInfo.getProduces() != null) {
                for (String produces : resourceInfo.getProduces()) {
                    if (produces.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON)) {
                        isJsonResponse = true;
                        break;
                    }
                }
            }
            if (!isJsonResponse) {
                continue;
            }

            ClassInfo effectiveReturnClassInfo = getEffectiveClassInfo(methodInfo.returnType(), indexView);
            if (effectiveReturnClassInfo == null) {
                continue;
            }
            AtomicBoolean needToDeleteCache = new AtomicBoolean(false);
            if (hasSecureFields(indexView, effectiveReturnClassInfo, typeToHasSecureField, needToDeleteCache)) {
                AnnotationInstance customSerializationAtClassAnnotation = methodInfo.declaringClass()
                        .declaredAnnotation(CUSTOM_SERIALIZATION);
                AnnotationInstance customSerializationAtMethodAnnotation = methodInfo.annotation(CUSTOM_SERIALIZATION);
                if (customSerializationAtMethodAnnotation != null || customSerializationAtClassAnnotation != null) {
                    log.warn("Secure serialization will not be applied to method: '" + methodInfo.declaringClass().name() + "#"
                            + methodInfo.name() + "' because the method or class are annotated with @CustomSerialization.");
                } else {
                    result.add(new ResourceMethodCustomSerializationBuildItem(methodInfo, entry.getActualClassInfo(),
                            SecurityCustomSerialization.class));
                }
            }
            if (needToDeleteCache.get()) {
                typeToHasSecureField.clear();
                typeToHasSecureField.putAll(getTypesWithSecureField());
            }
        }
        if (!result.isEmpty()) {
            for (ResourceMethodCustomSerializationBuildItem bi : result) {
                producer.produce(bi);
            }
        }
    }

    private static ClassInfo getEffectiveClassInfo(Type type, IndexView indexView) {
        if (type.kind() == Type.Kind.VOID) {
            return null;
        }
        Type effectiveReturnType = getEffectiveType(type);
        return effectiveReturnType == null ? null : indexView.getClassByName(effectiveReturnType.name());
    }

    private static Type getEffectiveType(Type type) {
        Type effectiveReturnType = type;
        if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.REST_RESPONSE) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.UNI) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.COMPLETABLE_FUTURE) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.COMPLETION_STAGE) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.REST_MULTI) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.MULTI)) {
            if (effectiveReturnType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                return null;
            }

            effectiveReturnType = type.asParameterizedType().arguments().get(0);
        }
        if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.SET) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.COLLECTION) ||
                effectiveReturnType.name().equals(ResteasyReactiveDotNames.LIST)) {
            effectiveReturnType = effectiveReturnType.asParameterizedType().arguments().get(0);
        } else if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.MAP)) {
            effectiveReturnType = effectiveReturnType.asParameterizedType().arguments().get(1);
        }
        return effectiveReturnType;
    }

    private static Map<String, Boolean> getTypesWithSecureField() {
        // if any of following types is detected as an endpoint return type or a field of endpoint return type,
        // we always need to apply security serialization as any type can be represented with them
        return Map.of(ResteasyReactiveDotNames.OBJECT.toString(), Boolean.TRUE, ResteasyReactiveDotNames.RESPONSE.toString(),
                Boolean.TRUE);
    }

    private static boolean hasSecureFields(IndexView indexView, ClassInfo currentClassInfo,
            Map<String, Boolean> typeToHasSecureField, AtomicBoolean needToDeleteCache) {
        // use cached result if there is any
        final String className = currentClassInfo.name().toString();
        if (typeToHasSecureField.containsKey(className)) {
            Boolean hasSecureFields = typeToHasSecureField.get(className);
            if (hasSecureFields == null) {
                // this is to avoid false negative for scenario like:
                // when 'a' declares field of type 'b' which declares field of type 'a' and both 'a' and 'b'
                // are returned from an endpoint and 'b' is detected based on 'a' and processed after 'a'
                needToDeleteCache.set(true);
                return false;
            }
            return hasSecureFields;
        }

        // prevent cyclic check of the same type
        // for example when a field has a same type as the current class has
        typeToHasSecureField.put(className, null);

        final boolean hasSecureFields;
        if (currentClassInfo.isInterface()) {
            if (isExcludedFromSecureFieldLookup(currentClassInfo.name())) {
                hasSecureFields = false;
            } else {
                // check interface implementors as anyone of them can be returned
                hasSecureFields = indexView.getAllKnownImplementors(currentClassInfo.name()).stream()
                        .anyMatch(ci -> hasSecureFields(indexView, ci, typeToHasSecureField, needToDeleteCache));
            }
        } else {
            // figure if any field or parent / subclass field is secured
            if (hasSecureFields(currentClassInfo)) {
                hasSecureFields = true;
            } else {
                if (isExcludedFromSecureFieldLookup(currentClassInfo.name())) {
                    hasSecureFields = false;
                } else {
                    hasSecureFields = anyFieldHasSecureFields(indexView, currentClassInfo, typeToHasSecureField,
                            needToDeleteCache)
                            || anySubclassHasSecureFields(indexView, currentClassInfo, typeToHasSecureField, needToDeleteCache)
                            || anyParentClassHasSecureFields(indexView, currentClassInfo, typeToHasSecureField,
                                    needToDeleteCache);
                }
            }
        }
        typeToHasSecureField.put(className, hasSecureFields);
        return hasSecureFields;
    }

    private static boolean isExcludedFromSecureFieldLookup(DotName name) {
        return ((Predicate<DotName>) QuarkusResteasyReactiveDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE).test(name);
    }

    private static boolean hasSecureFields(ClassInfo classInfo) {
        return classInfo.annotationsMap().containsKey(SECURE_FIELD);
    }

    private static boolean anyParentClassHasSecureFields(IndexView indexView, ClassInfo currentClassInfo,
            Map<String, Boolean> typeToHasSecureField, AtomicBoolean needToDeleteCache) {
        if (!currentClassInfo.superName().equals(ResteasyReactiveDotNames.OBJECT)) {
            final ClassInfo parentClassInfo = indexView.getClassByName(currentClassInfo.superName());
            return parentClassInfo != null
                    && hasSecureFields(indexView, parentClassInfo, typeToHasSecureField, needToDeleteCache);
        }
        return false;
    }

    private static boolean anySubclassHasSecureFields(IndexView indexView, ClassInfo currentClassInfo,
            Map<String, Boolean> typeToHasSecureField, AtomicBoolean needToDeleteCache) {
        return indexView.getAllKnownSubclasses(currentClassInfo.name()).stream()
                .anyMatch(subclass -> hasSecureFields(indexView, subclass, typeToHasSecureField, needToDeleteCache));
    }

    private static boolean anyFieldHasSecureFields(IndexView indexView, ClassInfo currentClassInfo,
            Map<String, Boolean> typeToHasSecureField, AtomicBoolean needToDeleteCache) {
        return currentClassInfo
                .fields()
                .stream()
                .filter(fieldInfo -> !fieldInfo.hasAnnotation(JSON_IGNORE))
                .map(FieldInfo::type)
                .anyMatch(fieldType -> fieldTypeHasSecureFields(fieldType, indexView, typeToHasSecureField, needToDeleteCache));
    }

    private static boolean fieldTypeHasSecureFields(Type fieldType, IndexView indexView,
            Map<String, Boolean> typeToHasSecureField, AtomicBoolean needToDeleteCache) {
        // this is the best effort and does not cover every possibility (e.g. type variables, wildcards)
        if (fieldType.kind() == Type.Kind.CLASS) {
            if (isExcludedFromSecureFieldLookup(fieldType.name())) {
                return false;
            }
            final ClassInfo fieldClass = indexView.getClassByName(fieldType.name());
            return fieldClass != null && hasSecureFields(indexView, fieldClass, typeToHasSecureField, needToDeleteCache);
        }
        if (fieldType.kind() == Type.Kind.ARRAY) {
            return fieldTypeHasSecureFields(fieldType.asArrayType().constituent(), indexView, typeToHasSecureField,
                    needToDeleteCache);
        }
        if (fieldType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            return fieldType
                    .asParameterizedType()
                    .arguments()
                    .stream()
                    .anyMatch(t -> fieldTypeHasSecureFields(t, indexView, typeToHasSecureField, needToDeleteCache));
        }
        return false;
    }

    private String getTargetId(AnnotationTarget target) {
        if (target.kind() == AnnotationTarget.Kind.CLASS) {
            return getClassId(target.asClass());
        } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
            return getMethodId(target.asMethod());
        }

        throw new UnsupportedOperationException("The `@CustomSerialization` and `@CustomDeserialization` annotations can only "
                + "be used in methods or classes.");
    }

    private String getClassId(ClassInfo classInfo) {
        return classInfo.name().toString();
    }

    private String getMethodId(MethodInfo methodInfo) {
        return getMethodId(methodInfo, methodInfo.declaringClass());
    }

    private String getMethodId(MethodInfo methodInfo, ClassInfo declaringClassInfo) {
        List<String> parameterClassNames = new ArrayList<>(methodInfo.parametersCount());
        for (Type parameter : methodInfo.parameterTypes()) {
            parameterClassNames.add(parameter.name().toString());
        }
        return MethodId.get(methodInfo.name(), declaringClassInfo.name().toString(),
                parameterClassNames.toArray(EMPTY_STRING_ARRAY));
    }

    /**
     * Purely marker build item so that we know at least one allowed role with configuration
     * expressions has been detected.
     */
    public static final class InitAndValidateRolesAllowedConfigExp extends SimpleBuildItem {
        private InitAndValidateRolesAllowedConfigExp() {
        }
    }
}
