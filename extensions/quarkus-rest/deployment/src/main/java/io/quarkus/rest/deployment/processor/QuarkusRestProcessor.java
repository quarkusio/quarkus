package io.quarkus.rest.deployment.processor;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.rest.deployment.framework.EndpointIndexer;
import io.quarkus.rest.deployment.framework.QuarkusRestDotNames;
import io.quarkus.rest.runtime.QuarkusRestConfig;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.ExceptionMapping;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.injection.ContextProducers;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.runtime.model.ResourceInterceptors;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.InputStreamMessageBodyReader;
import io.quarkus.rest.runtime.providers.serialisers.JsonbMessageBodyReader;
import io.quarkus.rest.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.quarkus.rest.runtime.providers.serialisers.VertxJsonMessageBodyWriter;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.vertx.core.buffer.Buffer;

public class QuarkusRestProcessor {

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem(Feature.QUARKUS_REST);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.QUARKUS_REST);
    }

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class).build());
        return new AutoInjectAnnotationBuildItem(DotName.createSimple(Context.class.getName()));

    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public FilterBuildItem setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            QuarkusRestConfig config,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            QuarkusRestRecorder recorder,
            ShutdownContextBuildItem shutdownContext) {
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> paths = index.getAnnotations(QuarkusRestDotNames.PATH);
        Collection<ClassInfo> containerRequestFilters = index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_REQUEST_FILTER);
        Collection<ClassInfo> containerResponseFilters = index
                .getAllKnownImplementors(QuarkusRestDotNames.CONTAINER_RESPONSE_FILTER);
        Collection<ClassInfo> exceptionMappers = index
                .getAllKnownImplementors(QuarkusRestDotNames.EXCEPTION_MAPPER);
        Collection<ClassInfo> writers = index
                .getAllKnownImplementors(QuarkusRestDotNames.MESSAGE_BODY_WRITER);
        Collection<ClassInfo> readers = index
                .getAllKnownImplementors(QuarkusRestDotNames.MESSAGE_BODY_READER);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return null;
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Map<DotName, String> scannedResourcePaths = new HashMap<>();
        Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
        Map<DotName, String> pathInterfaces = new HashMap<>();
        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                    scannedResourcePaths.put(clazz.name(), annotation.value().asString());
                } else {
                    pathInterfaces.put(clazz.name(), annotation.value().asString());
                }
            }
        }
        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            for (ClassInfo clazz : index.getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        scannedResources.put(clazz.name(), clazz);
                        scannedResourcePaths.put(clazz.name(), i.getValue());
                    }
                }
            }
        }
        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        for (ClassInfo i : scannedResources.values()) {
            ResourceClass endpoints = EndpointIndexer.createEndpoints(index, i,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths, config);
            if (endpoints != null) {
                resourceClasses.add(endpoints);
            }
        }

        //now index possible sub resources. These are all classes that have method annotations
        //that are not annotated @Path
        //TODO custom method annotations
        Deque<ClassInfo> toScan = new ArrayDeque<>();
        for (DotName methodAnnotation : QuarkusRestDotNames.JAXRS_METHOD_ANNOTATIONS) {
            for (AnnotationInstance instance : index.getAnnotations(methodAnnotation)) {
                MethodInfo method = instance.target().asMethod();
                ClassInfo classInfo = method.declaringClass();
                toScan.add(classInfo);
            }
        }
        while (!toScan.isEmpty()) {
            ClassInfo classInfo = toScan.poll();
            if (scannedResources.containsKey(classInfo.name()) ||
                    pathInterfaces.containsKey(classInfo.name()) ||
                    possibleSubResources.containsKey(classInfo.name())) {
                continue;
            }
            possibleSubResources.put(classInfo.name(), classInfo);
            ResourceClass endpoints = EndpointIndexer.createEndpoints(index, classInfo,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths, config);
            if (endpoints != null) {
                subResourceClasses.add(endpoints);
            }
            //we need to also look for all sub classes and interfaces
            //they may have type variables that need to be handled
            toScan.addAll(index.getKnownDirectImplementors(classInfo.name()));
            toScan.addAll(index.getKnownDirectSubclasses(classInfo.name()));
        }

        ResourceInterceptors interceptors = new ResourceInterceptors();
        for (ClassInfo filterClass : containerRequestFilters) {
            if (filterClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceRequestInterceptor interceptor = new ResourceRequestInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                interceptor.setPreMatching(filterClass.classAnnotation(QuarkusRestDotNames.PRE_MATCHING) != null);
                if (interceptor.isPreMatching()) {
                    interceptors.addResourcePreMatchInterceptor(interceptor);
                } else {
                    Set<String> nameBindingNames = EndpointIndexer.nameBindingNames(filterClass, index);
                    if (nameBindingNames.isEmpty()) {
                        interceptors.addGlobalRequestInterceptor(interceptor);
                    } else {
                        interceptor.setNameBindingNames(nameBindingNames);
                        interceptors.addNameRequestInterceptor(interceptor);
                    }
                }
                AnnotationInstance priorityInstance = filterClass.classAnnotation(QuarkusRestDotNames.PRIORITY);
                if (priorityInstance != null) {
                    interceptor.setPriority(priorityInstance.value().asInt());
                }
            }
        }
        for (ClassInfo filterClass : containerResponseFilters) {
            if (filterClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceResponseInterceptor interceptor = new ResourceResponseInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                Set<String> nameBindingNames = EndpointIndexer.nameBindingNames(filterClass, index);
                if (nameBindingNames.isEmpty()) {
                    interceptors.addGlobalResponseInterceptor(interceptor);
                } else {
                    interceptor.setNameBindingNames(nameBindingNames);
                    interceptors.addNameResponseInterceptor(interceptor);
                }
                AnnotationInstance priorityInstance = filterClass.classAnnotation(QuarkusRestDotNames.PRIORITY);
                if (priorityInstance != null) {
                    interceptor.setPriority(priorityInstance.value().asInt());
                }
            }
        }

        ExceptionMapping exceptionMapping = new ExceptionMapping();
        for (ClassInfo mapperClass : exceptionMappers) {
            if (mapperClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(),
                        QuarkusRestDotNames.EXCEPTION_MAPPER,
                        index);
                ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
                mapper.setFactory(recorder.factory(mapperClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerExceptionMapper(exceptionMapping, typeParameters.get(0).name().toString(), mapper);
            }
        }

        Serialisers serialisers = new Serialisers();
        for (ClassInfo writerClass : writers) {
            if (writerClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                ResourceWriter writer = new ResourceWriter();
                AnnotationInstance producesAnnotation = writerClass.classAnnotation(QuarkusRestDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    writer.setMediaTypeStrings(Arrays.asList(producesAnnotation.value().asStringArray()));
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        QuarkusRestDotNames.MESSAGE_BODY_WRITER,
                        index);
                writer.setFactory(recorder.factory(writerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerWriter(serialisers, typeParameters.get(0).name().toString(), writer);
            }
        }
        for (ClassInfo readerClass : readers) {
            if (readerClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        QuarkusRestDotNames.MESSAGE_BODY_READER,
                        index);
                ResourceReader reader = new ResourceReader();
                reader.setFactory(recorder.factory(readerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerReader(serialisers, typeParameters.get(0).name().toString(), reader);
            }
        }

        // built-ins
        //        registerWriter(recorder, serialisers, Object.class, JsonbMessageBodyWriter.class, beanContainerBuildItem.getValue(),
        //                false);
        registerWriter(recorder, serialisers, Object.class, VertxJsonMessageBodyWriter.class, beanContainerBuildItem.getValue(),
                MediaType.APPLICATION_JSON);
        registerWriter(recorder, serialisers, String.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Number.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Boolean.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Character.class, StringMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, Object.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, char[].class, CharArrayMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.TEXT_PLAIN);
        registerWriter(recorder, serialisers, byte[].class, ByteArrayMessageBodyHandler.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, Buffer.class, VertxBufferMessageBodyWriter.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);

        registerReader(recorder, serialisers, String.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, InputStream.class, InputStreamMessageBodyReader.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, Object.class, JsonbMessageBodyReader.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);

        return new FilterBuildItem(
                recorder.handler(interceptors.sort(), exceptionMapping, serialisers, resourceClasses, subResourceClasses,
                        shutdownContext, config),
                10);
    }

    private void registerWriter(QuarkusRestRecorder recorder, Serialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyWriter<?>> writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass.getName(), beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    private <T> void registerReader(QuarkusRestRecorder recorder, Serialisers serialisers, Class<T> entityClass,
            Class<? extends MessageBodyReader<T>> readerClass, BeanContainer beanContainer, String mediaType) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass.getName(), beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerReader(serialisers, entityClass.getName(), reader);

    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QuarkusRestDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }
}
