package io.quarkus.rest.client.reactive.kotlin.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_KOTLIN_SERIALIZATION;

import java.util.Collections;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.rest.client.reactive.kotlin.runtime.serializers.ClientKotlinMessageBodyReader;
import io.quarkus.rest.client.reactive.kotlin.runtime.serializers.ClientKotlinMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

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
                        Collections.singletonList(MediaType.APPLICATION_JSON), RuntimeType.CLIENT, true, Priorities.USER));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(ClientKotlinMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON), RuntimeType.CLIENT, true, Priorities.USER));
    }
}
