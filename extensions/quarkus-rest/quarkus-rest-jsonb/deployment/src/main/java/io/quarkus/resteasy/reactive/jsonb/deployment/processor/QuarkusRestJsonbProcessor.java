package io.quarkus.resteasy.reactive.jsonb.deployment.processor;

import java.util.Collections;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.jsonb.runtime.serialisers.JsonbMessageBodyReader;
import io.quarkus.resteasy.reactive.jsonb.runtime.serialisers.JsonbMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class QuarkusRestJsonbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.QUARKUS_REST_JSONB));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonbMessageBodyReader.class.getName())
                .addBeanClass(JsonbMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders.produce(new MessageBodyReaderBuildItem(JsonbMessageBodyReader.class.getName(), Object.class.getName(),
                Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters.produce(new MessageBodyWriterBuildItem(JsonbMessageBodyWriter.class.getName(), Object.class.getName(),
                Collections.singletonList(MediaType.APPLICATION_JSON)));
    }
}
