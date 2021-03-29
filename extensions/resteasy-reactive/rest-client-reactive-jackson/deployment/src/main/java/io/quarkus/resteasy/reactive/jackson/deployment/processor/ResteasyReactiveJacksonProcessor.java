package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonBasicMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.JacksonBasicMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayBasicMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayBasicMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectBasicMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectBasicMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResteasyReactiveJacksonProcessor {

    @BuildStep
    void additionalProviders(
            List<ResteasyReactiveJacksonProviderDefinedBuildItem> jacksonProviderDefined,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        if (jacksonProviderDefined != null && !jacksonProviderDefined.isEmpty()) {
            return;
        }
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JacksonBasicMessageBodyReader.class.getName())
                .addBeanClass(JacksonBasicMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(JacksonBasicMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonArrayBasicMessageBodyReader.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalReaders
                .produce(new MessageBodyReaderBuildItem(VertxJsonObjectBasicMessageBodyReader.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(JacksonBasicMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonArrayBasicMessageBodyWriter.class.getName(),
                        JsonArray.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(VertxJsonObjectBasicMessageBodyWriter.class.getName(),
                        JsonObject.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
    }
}
