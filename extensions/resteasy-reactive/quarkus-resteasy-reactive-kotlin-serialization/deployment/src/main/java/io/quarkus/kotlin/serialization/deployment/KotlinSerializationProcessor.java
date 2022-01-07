package io.quarkus.kotlin.serialization.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_KOTLIN_SERIALIZATION;
import static io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem.json;

import java.util.List;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.kotlin.serialization.KotlinSerializationMessageBodyReader;
import io.quarkus.kotlin.serialization.KotlinSerializationMessageBodyWriter;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class KotlinSerializationProcessor {
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
                        MediaType.APPLICATION_JSON)));
        additionalWriters.produce(new MessageBodyWriterBuildItem(
                KotlinSerializationMessageBodyWriter.class.getName(), Object.class.getName(), List.of(
                        MediaType.APPLICATION_JSON)));
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
