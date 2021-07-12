package io.quarkus.kotlin.serialization.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_KOTLIN_SERIALIZATION;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem.json;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.kotlin.serialization.KotSerMessageBodyReader;
import io.quarkus.kotlin.serialization.KotSerMessageBodyWriter;
import io.quarkus.kotlin.serialization.KotlinSerializationConfig;
import io.quarkus.kotlin.serialization.SerializerRecorder;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import kotlinx.serialization.json.Json;

public class KotlinSerializationProcessor {
    @BuildStep
    public void additionalProviders(
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(KotSerMessageBodyReader.class.getName())
                .addBeanClass(KotSerMessageBodyWriter.class.getName())
                .setUnremovable().build());
        additionalReaders.produce(new MessageBodyReaderBuildItem(
                KotSerMessageBodyReader.class.getName(), Object.class.getName(), List.of(
                        MediaType.APPLICATION_JSON)));
        additionalWriters.produce(new MessageBodyWriterBuildItem(
                KotSerMessageBodyWriter.class.getName(), Object.class.getName(), List.of(
                        MediaType.APPLICATION_JSON)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public SyntheticBeanBuildItem createJson(SerializerRecorder recorder, KotlinSerializationConfig config) {
        return SyntheticBeanBuildItem
                .configure(Json.class)
                .scope(Singleton.class)
                .supplier(recorder.configFactory(config))
                .unremovable().done();
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
