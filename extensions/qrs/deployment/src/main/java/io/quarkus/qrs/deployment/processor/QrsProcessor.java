package io.quarkus.qrs.deployment.processor;

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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.qrs.deployment.framework.EndpointIndexer;
import io.quarkus.qrs.deployment.framework.QrsDotNames;
import io.quarkus.qrs.runtime.QrsRecorder;
import io.quarkus.qrs.runtime.core.ExceptionMapping;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.model.ResourceClass;
import io.quarkus.qrs.runtime.model.ResourceExceptionMapper;
import io.quarkus.qrs.runtime.model.ResourceInterceptors;
import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceRequestInterceptor;
import io.quarkus.qrs.runtime.model.ResourceResponseInterceptor;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.providers.serialisers.JsonbMessageBodyReader;
import io.quarkus.qrs.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.qrs.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.quarkus.qrs.runtime.providers.serialisers.VertxJsonMessageBodyWriter;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.vertx.core.buffer.Buffer;

public class QrsProcessor {

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem("qrs");
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public FilterBuildItem setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            QrsRecorder recorder,
            ShutdownContextBuildItem shutdownContext) {
        Collection<AnnotationInstance> paths = beanArchiveIndexBuildItem.getIndex().getAnnotations(QrsDotNames.PATH);
        Collection<ClassInfo> containerRequestFilters = beanArchiveIndexBuildItem.getIndex()
                .getAllKnownImplementors(QrsDotNames.CONTAINER_REQUEST_FILTER);
        Collection<ClassInfo> containerResponseFilters = beanArchiveIndexBuildItem.getIndex()
                .getAllKnownImplementors(QrsDotNames.CONTAINER_RESPONSE_FILTER);
        Collection<ClassInfo> exceptionMappers = beanArchiveIndexBuildItem.getIndex()
                .getAllKnownImplementors(QrsDotNames.EXCEPTION_MAPPER);
        Collection<ClassInfo> writers = beanArchiveIndexBuildItem.getIndex()
                .getAllKnownImplementors(QrsDotNames.MESSAGE_BODY_WRITER);
        Collection<ClassInfo> readers = beanArchiveIndexBuildItem.getIndex()
                .getAllKnownImplementors(QrsDotNames.MESSAGE_BODY_READER);

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
            for (ClassInfo clazz : beanArchiveIndexBuildItem.getIndex().getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                    scannedResourcePaths.put(clazz.name(), i.getValue());
                }
            }
        }
        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        for (ClassInfo i : scannedResources.values()) {
            ResourceClass endpoints = EndpointIndexer.createEndpoints(beanArchiveIndexBuildItem.getIndex(), i,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths);
            if (endpoints != null) {
                resourceClasses.add(endpoints);
            }
        }

        //now index possible sub resources. These are all classes that have method annotations
        //that are not annotated @Path
        //TODO custom method annotations
        Deque<ClassInfo> toScan = new ArrayDeque<>();
        for (DotName methodAnnotation : QrsDotNames.JAXRS_METHOD_ANNOTATIONS) {
            for (AnnotationInstance instance : beanArchiveIndexBuildItem.getIndex().getAnnotations(methodAnnotation)) {
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
            ResourceClass endpoints = EndpointIndexer.createEndpoints(beanArchiveIndexBuildItem.getIndex(), classInfo,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder, existingConverters,
                    scannedResourcePaths);
            if (endpoints != null) {
                subResourceClasses.add(endpoints);
            }
            //we need to also look for all sub classes and interfaces
            //they may have type variables that need to be handled
            toScan.addAll(beanArchiveIndexBuildItem.getIndex().getKnownDirectImplementors(classInfo.name()));
            toScan.addAll(beanArchiveIndexBuildItem.getIndex().getKnownDirectSubclasses(classInfo.name()));
        }

        ResourceInterceptors interceptors = new ResourceInterceptors();
        for (ClassInfo filterClass : containerRequestFilters) {
            if (filterClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                ResourceRequestInterceptor interceptor = new ResourceRequestInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                interceptor.setPreMatching(filterClass.classAnnotation(QrsDotNames.PRE_MATCHING) != null);
                if (interceptor.isPreMatching()) {
                    interceptors.addResourcePreMatchInterceptor(interceptor);
                } else {
                    interceptors.addRequestInterceptor(interceptor);
                }
            }
        }
        for (ClassInfo filterClass : containerResponseFilters) {
            if (filterClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                ResourceResponseInterceptor interceptor = new ResourceResponseInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                interceptors.addResponseInterceptor(interceptor);
            }
        }

        ExceptionMapping exceptionMapping = new ExceptionMapping();
        for (ClassInfo mapperClass : exceptionMappers) {
            if (mapperClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(mapperClass.name(), QrsDotNames.EXCEPTION_MAPPER,
                        beanArchiveIndexBuildItem.getIndex());
                ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
                mapper.setFactory(recorder.factory(mapperClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerExceptionMapper(exceptionMapping, typeParameters.get(0).name().toString(), mapper);
            }
        }

        Serialisers serialisers = new Serialisers();
        for (ClassInfo writerClass : writers) {
            if (writerClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                ResourceWriter writer = new ResourceWriter();
                AnnotationInstance producesAnnotation = writerClass.classAnnotation(QrsDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    writer.setMediaTypeStrings(Arrays.asList(producesAnnotation.value().asStringArray()));
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        QrsDotNames.MESSAGE_BODY_WRITER,
                        beanArchiveIndexBuildItem.getIndex());
                writer.setFactory(recorder.factory(writerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerWriter(serialisers, typeParameters.get(0).name().toString(), writer);
            }
        }
        for (ClassInfo readerClass : readers) {
            if (readerClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        QrsDotNames.MESSAGE_BODY_READER,
                        beanArchiveIndexBuildItem.getIndex());
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
        registerWriter(recorder, serialisers, byte[].class, ByteArrayMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerWriter(recorder, serialisers, Buffer.class, VertxBufferMessageBodyWriter.class,
                beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);

        registerReader(recorder, serialisers, String.class, StringMessageBodyHandler.class, beanContainerBuildItem.getValue(),
                MediaType.WILDCARD);
        registerReader(recorder, serialisers, Object.class, JsonbMessageBodyReader.class, beanContainerBuildItem.getValue(),
                MediaType.APPLICATION_JSON);

        return new FilterBuildItem(
                recorder.handler(interceptors, exceptionMapping, serialisers, resourceClasses, subResourceClasses,
                        shutdownContext),
                10);
    }

    private void registerWriter(QrsRecorder recorder, Serialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyWriter<?>> writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass.getName(), beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    private <T> void registerReader(QrsRecorder recorder, Serialisers serialisers, Class<T> entityClass,
            Class<? extends MessageBodyReader<T>> readerClass, BeanContainer beanContainer, String mediaType) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass.getName(), beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerReader(serialisers, entityClass.getName(), reader);

    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QrsDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QrsDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QrsDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }
}
