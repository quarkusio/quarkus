package io.quarkus.rest.client.reactive.jackson.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_JACKSON;
import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_NDJSON;
import static org.jboss.resteasy.reactive.common.util.RestMediaType.APPLICATION_STREAM_JSON;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyReader;
import io.quarkus.rest.client.reactive.jackson.runtime.serialisers.ClientJacksonMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.deployment.processor.ResteasyReactiveJacksonProviderDefinedBuildItem;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayBasicMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonArrayBasicMessageBodyWriter;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectBasicMessageBodyReader;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx.VertxJsonObjectBasicMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RestClientReactiveJacksonProcessor {

    private static final List<String> HANDLED_WRITE_MEDIA_TYPES = Collections.singletonList(MediaType.APPLICATION_JSON);
    private static final List<String> HANDLED_READ_MEDIA_TYPES = List.of(MediaType.APPLICATION_JSON, APPLICATION_NDJSON,
            APPLICATION_STREAM_JSON);

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_REACTIVE_JACKSON));
    }

    @BuildStep
    void additionalProviders(
            List<ResteasyReactiveJacksonProviderDefinedBuildItem> jacksonProviderDefined,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        if (!jacksonProviderDefined.isEmpty()) {
            return;
        }
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ClientJacksonMessageBodyReader.class.getName())
                .addBeanClass(ClientJacksonMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(ClientJacksonMessageBodyReader.class.getName(),
                                Object.class.getName())
                                .setMediaTypeStrings(HANDLED_READ_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(VertxJsonArrayBasicMessageBodyReader.class.getName(),
                                JsonArray.class.getName())
                                .setMediaTypeStrings(HANDLED_READ_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
        additionalReaders
                .produce(
                        new MessageBodyReaderBuildItem.Builder(VertxJsonObjectBasicMessageBodyReader.class.getName(),
                                JsonObject.class.getName())
                                .setMediaTypeStrings(HANDLED_READ_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(ClientJacksonMessageBodyWriter.class.getName(),
                                Object.class.getName())
                                .setMediaTypeStrings(HANDLED_WRITE_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(VertxJsonArrayBasicMessageBodyWriter.class.getName(),
                                JsonArray.class.getName())
                                .setMediaTypeStrings(HANDLED_WRITE_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
        additionalWriters
                .produce(
                        new MessageBodyWriterBuildItem.Builder(VertxJsonObjectBasicMessageBodyWriter.class.getName(),
                                JsonObject.class.getName())
                                .setMediaTypeStrings(HANDLED_WRITE_MEDIA_TYPES)
                                .setBuiltin(true)
                                .build());
    }
}
