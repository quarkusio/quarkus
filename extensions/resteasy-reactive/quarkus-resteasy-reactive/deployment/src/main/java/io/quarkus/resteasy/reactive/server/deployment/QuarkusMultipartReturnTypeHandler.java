package io.quarkus.resteasy.reactive.server.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartMessageBodyWriter;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class QuarkusMultipartReturnTypeHandler implements EndpointIndexer.MultipartReturnTypeIndexerExtension {
    private final Map<String, Boolean> multipartOutputGeneratedPopulators = new HashMap<>();
    final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    final Predicate<String> applicationClassPredicate;
    final BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer;

    public QuarkusMultipartReturnTypeHandler(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            Predicate<String> applicationClassPredicate, BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
        this.applicationClassPredicate = applicationClassPredicate;
        this.reflectiveClassProducer = reflectiveClassProducer;
    }

    @Override
    public boolean handleMultipartForReturnType(AdditionalWriters additionalWriters, ClassInfo multipartClassInfo,
            IndexView index) {

        String className = multipartClassInfo.name().toString();
        Boolean canHandle = multipartOutputGeneratedPopulators.get(className);
        if (canHandle != null) {
            // we've already seen this class before and have done all we need
            return canHandle;
        }

        canHandle = false;
        if (FormDataOutputMapperGenerator.isReturnTypeCompatible(multipartClassInfo, index)) {
            additionalWriters.add(MultipartMessageBodyWriter.class.getName(), MediaType.MULTIPART_FORM_DATA, className);
            String mapperClassName = FormDataOutputMapperGenerator.generate(multipartClassInfo,
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer,
                            applicationClassPredicate.test(className)),
                    index);
            reflectiveClassProducer.produce(
                    new ReflectiveClassBuildItem(true, false, MultipartMessageBodyWriter.class.getName()));
            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(false, false, className));
            reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, false, mapperClassName));
            canHandle = true;
        }

        multipartOutputGeneratedPopulators.put(className, canHandle);
        return canHandle;
    }
}
