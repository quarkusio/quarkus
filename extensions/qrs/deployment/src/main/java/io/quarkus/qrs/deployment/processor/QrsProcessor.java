package io.quarkus.qrs.deployment.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
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
import io.quarkus.qrs.runtime.providers.serialisers.JsonbMessageBodyWriter;
import io.quarkus.qrs.runtime.providers.serialisers.StringMessageBodyWriter;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

public class QrsProcessor {

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem("qrs");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public FilterBuildItem setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            QrsRecorder recorder, ExecutorBuildItem executorBuildItem) {
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
        Set<DotName> pathInterfaces = new HashSet<>();
        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                } else {
                    pathInterfaces.add(clazz.name());
                }
            }
        }

        List<ResourceClass> resourceClasses = new ArrayList<>();
        for (ClassInfo i : scannedResources.values()) {
            resourceClasses.add(EndpointIndexer.createEndpoints(beanArchiveIndexBuildItem.getIndex(), i,
                    beanContainerBuildItem.getValue(), generatedClassBuildItemBuildProducer, recorder));
        }

        ResourceInterceptors interceptors = new ResourceInterceptors();
        for (ClassInfo filterClass : containerRequestFilters) {
            if (filterClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                ResourceRequestInterceptor interceptor = new ResourceRequestInterceptor();
                interceptor.setFactory(recorder.factory(filterClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                interceptors.addRequestInterceptor(interceptor);
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
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        QrsDotNames.MESSAGE_BODY_WRITER,
                        beanArchiveIndexBuildItem.getIndex());
                ResourceWriter<?> writer = new ResourceWriter<>();
                writer.setFactory(recorder.factory(writerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerWriter(serialisers, typeParameters.get(0).name().toString(), writer);
            }
        }
        for (ClassInfo readerClass : readers) {
            if (readerClass.classAnnotation(QrsDotNames.PROVIDER) != null) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        QrsDotNames.MESSAGE_BODY_WRITER,
                        beanArchiveIndexBuildItem.getIndex());
                ResourceReader<?> reader = new ResourceReader<>();
                reader.setFactory(recorder.factory(readerClass.name().toString(),
                        beanContainerBuildItem.getValue()));
                recorder.registerReader(serialisers, typeParameters.get(0).name().toString(), reader);
            }
        }

        // built-ins
        registerWriter(recorder, serialisers, String.class, StringMessageBodyWriter.class, beanContainerBuildItem.getValue());
        registerWriter(recorder, serialisers, Object.class, JsonbMessageBodyWriter.class, beanContainerBuildItem.getValue());

        return new FilterBuildItem(
                recorder.handler(interceptors, exceptionMapping, serialisers, resourceClasses,
                        executorBuildItem.getExecutorProxy()),
                10);
    }

    private <T> void registerWriter(QrsRecorder recorder, Serialisers serialisers, Class<T> entityClass,
            Class<? extends MessageBodyWriter<T>> writerClass, BeanContainer beanContainer) {
        ResourceWriter<Object> writer = new ResourceWriter<>();
        writer.setFactory(recorder.factory(writerClass.getName().toString(), beanContainer));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QrsDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QrsDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
    }
}
