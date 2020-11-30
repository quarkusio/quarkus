package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJacksonProcessor {

    private static final DotName JSON_VIEW = DotName.createSimple(JsonView.class.getName());

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JACKSON));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JacksonMessageBodyReader.class.getName())
                .addBeanClass(JacksonMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(JacksonMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(JacksonMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
    }

    @BuildStep
    void registerForReflection(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }

        Collection<ClassInfo> resourceClasses = resourceScanningResultBuildItem.get().getScannedResources().values();
        Set<String> classesNeedingReflectionOnMethods = new HashSet<>();
        for (ClassInfo resourceClass : resourceClasses) {
            if (resourceClass.annotations().containsKey(JSON_VIEW)) {
                classesNeedingReflectionOnMethods.add(resourceClass.name().toString());
            }
        }
        if (!classesNeedingReflectionOnMethods.isEmpty()) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, classesNeedingReflectionOnMethods.toArray(new String[0])));
        }
    }
}
