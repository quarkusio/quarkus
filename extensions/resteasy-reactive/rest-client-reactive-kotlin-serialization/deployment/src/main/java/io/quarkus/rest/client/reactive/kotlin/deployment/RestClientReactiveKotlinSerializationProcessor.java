package io.quarkus.rest.client.reactive.kotlin.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_KOTLIN_SERIALIZATION;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Collections;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.kotlin.serialization.KotlinSerializationConfig;
import io.quarkus.kotlin.serialization.KotlinSerializerRecorder;
import io.quarkus.rest.client.reactive.kotlin.runtime.serializers.ClientKotlinMessageBodyReader;
import io.quarkus.rest.client.reactive.kotlin.runtime.serializers.ClientKotlinMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import kotlinx.serialization.json.Json;

public class RestClientReactiveKotlinSerializationProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_REACTIVE_KOTLIN_SERIALIZATION));
    }

    @BuildStep
    void additionalProviders(
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ClientKotlinMessageBodyReader.class.getName())
                .addBeanClass(ClientKotlinMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(ClientKotlinMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(ClientKotlinMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public SyntheticBeanBuildItem createJson(KotlinSerializerRecorder recorder, KotlinSerializationConfig config) {
        return SyntheticBeanBuildItem
                .configure(Json.class)
                .scope(Singleton.class)
                .supplier(recorder.configFactory(config))
                .unremovable().done();
    }
}
