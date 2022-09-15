package io.quarkus.kotlin.serialization.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_KOTLIN_SERIALIZATION;
import static io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem.json;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.kotlin.serialization.KotlinSerializationMessageBodyReader;
import io.quarkus.kotlin.serialization.KotlinSerializationMessageBodyWriter;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class KotlinSerializationProcessor {

    private static final DotName SERIALIZABLE = DotName.createSimple("kotlinx.serialization.Serializable");
    private static final String COMPANION_CLASS_SUFFIX = "$Companion";

    // Kotlin Serialization generates classes at compile time which need to be available via reflection
    // for serialization to work properly
    @BuildStep
    public void registerReflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        var serializableInstances = index.getIndex().getAnnotations(SERIALIZABLE);
        if (serializableInstances.isEmpty()) {
            return;
        }

        List<String> supportClassNames = new ArrayList<>(2 * serializableInstances.size());
        for (AnnotationInstance instance : serializableInstances) {
            if (instance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            var targetClass = instance.target().asClass().name();
            String companionClassName = targetClass.toString() + COMPANION_CLASS_SUFFIX;
            supportClassNames.add(companionClassName);
        }
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, supportClassNames.toArray(new String[0])));
    }

    @BuildStep
    public void additionalProviders(
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(KotlinSerializationMessageBodyReader.class.getName())
                .addBeanClass(KotlinSerializationMessageBodyWriter.class.getName())
                .setUnremovable().build());
        additionalReaders.produce(new MessageBodyReaderBuildItem(
                KotlinSerializationMessageBodyReader.class.getName(), Object.class.getName(), List.of(
                        MediaType.APPLICATION_JSON),
                RuntimeType.SERVER, true, Priorities.USER));
        additionalWriters.produce(new MessageBodyWriterBuildItem(
                KotlinSerializationMessageBodyWriter.class.getName(), Object.class.getName(), List.of(
                        MediaType.APPLICATION_JSON),
                RuntimeType.SERVER, true, Priorities.USER));
    }

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(RESTEASY_REACTIVE_KOTLIN_SERIALIZATION));
    }

    @BuildStep
    public ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return json();
    }
}
