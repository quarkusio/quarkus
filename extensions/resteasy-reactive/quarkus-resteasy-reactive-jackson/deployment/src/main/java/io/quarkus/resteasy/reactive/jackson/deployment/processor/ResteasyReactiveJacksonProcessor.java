package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Collections;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResteasyReactiveJacksonProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_REACTIVE_JACKSON));
    }

    @BuildStep
    ServerDefaultProducesHandlerBuildItem jsonDefault() {
        return ServerDefaultProducesHandlerBuildItem.json();
    }

    @BuildStep
    ResteasyReactiveJacksonProviderDefinedBuildItem jacksonRegistered() {
        return new ResteasyReactiveJacksonProviderDefinedBuildItem();
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ServerJacksonMessageBodyReader.class.getName())
                .addBeanClass(ServerJacksonMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(ServerJacksonMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonArrayMessageBodyReader.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonObjectMessageBodyReader.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(ServerJacksonMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonArrayMessageBodyWriter.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonObjectMessageBodyWriter.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
    }
}
