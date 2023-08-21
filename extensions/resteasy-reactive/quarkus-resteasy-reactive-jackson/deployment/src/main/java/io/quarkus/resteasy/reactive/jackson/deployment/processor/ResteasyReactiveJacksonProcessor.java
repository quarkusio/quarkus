package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_NDJSON;
import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_STREAM_JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.JaxRsResourceIndexBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.EnableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.DefaultMismatchedInputException;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.NativeInvalidDefinitionExceptionMapper;
import io.quarkus.resteasy.reactive.jackson.runtime.security.SecurityCustomSerialization;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.BasicServerJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.FullyFeaturedServerJacksonMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.FullyFeaturedServerJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyReader;
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

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JACKSON));
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
    ExceptionMapperBuildItem exceptionMappers() {
        return new ExceptionMapperBuildItem(DefaultMismatchedInputException.class.getName(),
                MismatchedInputException.class.getName(), Priorities.USER + 100, false);
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
                    recorder.recordJsonView(getMethodId(instance.target().asMethod()), jsonViews[0].name().toString());
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
                                    .build());
                    recorder.recordCustomDeserialization(getTargetId(instance.target()), biFunctionType.name().toString());
                }
            }
        }

        for (ResourceMethodCustomSerializationBuildItem bi : resourceMethodCustomSerializationBuildItems) {
            jacksonFeatures.add(JacksonFeatureBuildItem.Feature.CUSTOM_SERIALIZATION);
            String className = bi.getCustomSerializationProvider().getName();
            reflectiveClassProducer.produce(
                    ReflectiveClassBuildItem.builder(className).build());
            recorder.recordCustomSerialization(getMethodId(bi.getMethodInfo(), bi.getDeclaringClassInfo()), className);
        }

        if (!jacksonFeatures.isEmpty()) {
            for (JacksonFeatureBuildItem.Feature jacksonFeature : jacksonFeatures) {
                jacksonFeaturesProducer.produce(new JacksonFeatureBuildItem(jacksonFeature));
            }
            recorder.configureShutdown(shutdown);
        }
    }

    @BuildStep
    public void handleFieldSecurity(ResteasyReactiveResourceMethodEntriesBuildItem resourceMethodEntries,
            JaxRsResourceIndexBuildItem index,
            BuildProducer<ResourceMethodCustomSerializationBuildItem> producer) {
        IndexView indexView = index.getIndexView();
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

            Type returnType = methodInfo.returnType();
            if (returnType.kind() == Type.Kind.VOID) {
                continue;
            }
            Type effectiveReturnType = returnType;
            if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.REST_RESPONSE) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.UNI) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.COMPLETABLE_FUTURE) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.COMPLETION_STAGE) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.REST_MULTI) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.MULTI)) {
                if (effectiveReturnType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                    continue;
                }

                effectiveReturnType = returnType.asParameterizedType().arguments().get(0);
            }
            if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.SET) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.COLLECTION) ||
                    effectiveReturnType.name().equals(ResteasyReactiveDotNames.LIST)) {
                effectiveReturnType = effectiveReturnType.asParameterizedType().arguments().get(0);
            } else if (effectiveReturnType.name().equals(ResteasyReactiveDotNames.MAP)) {
                effectiveReturnType = effectiveReturnType.asParameterizedType().arguments().get(1);
            }

            ClassInfo effectiveReturnClassInfo = indexView.getClassByName(effectiveReturnType.name());
            if ((effectiveReturnClassInfo == null) || effectiveReturnClassInfo.name().equals(ResteasyReactiveDotNames.OBJECT)) {
                continue;
            }
            ClassInfo currentClassInfo = effectiveReturnClassInfo;
            boolean hasSecureFields = false;
            while (true) {
                if (currentClassInfo.annotationsMap().containsKey(SECURE_FIELD)) {
                    hasSecureFields = true;
                    break;
                }
                if (currentClassInfo.superName().equals(ResteasyReactiveDotNames.OBJECT)) {
                    break;
                }
                currentClassInfo = indexView.getClassByName(currentClassInfo.superName());
                if (currentClassInfo == null) {
                    break;
                }
            }
            if (hasSecureFields) {
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
        }
        if (!result.isEmpty()) {
            for (ResourceMethodCustomSerializationBuildItem bi : result) {
                producer.produce(bi);
            }
        }
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
}
