package io.quarkus.resteasy.reactive.kotlin.serialization.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_KOTLIN_SERIALIZATION;
import static io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem.json;

import java.util.List;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.kotlin.serialization.runtime.KotlinSerializationMessageBodyReader;
import io.quarkus.resteasy.reactive.kotlin.serialization.runtime.KotlinSerializationMessageBodyWriter;
import io.quarkus.resteasy.reactive.kotlin.serialization.runtime.ValidationJsonBuilderCustomizer;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class KotlinSerializationProcessor {

    @BuildStep
    public void additionalProviders(
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters,
            Capabilities capabilities) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .addBeanClasses(KotlinSerializationMessageBodyReader.class.getName(),
                        KotlinSerializationMessageBodyWriter.class.getName());
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            builder.addBeanClass(ValidationJsonBuilderCustomizer.class.getName());
        }
        additionalBean.produce(builder.setUnremovable().build());
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
