package io.quarkus.resteasy.reactive.jsonb.common.deployment;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.jboss.resteasy.reactive.server.jsonb.JsonbMessageBodyReader;
import org.jboss.resteasy.reactive.server.jsonb.JsonbMessageBodyWriter;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.jsonb.spi.JsonbDeserializerBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.resteasy.reactive.jsonb.common.runtime.serialisers.VertxJson;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

public class ResteasyReactiveJsonbCommonProcessor {

    private static final List<String> VERTX_SERIALIZERS = List.of(
            VertxJson.JsonObjectSerializer.class.getName(),
            VertxJson.JsonArraySerializer.class.getName());

    private static final List<String> VERTX_DESERIALIZERS = List.of(
            VertxJson.JsonObjectDeserializer.class.getName(),
            VertxJson.JsonArrayDeserializer.class.getName());

    @BuildStep
    public void registerVertxJsonSupport(
            BuildProducer<JsonbSerializerBuildItem> serializers,
            BuildProducer<JsonbDeserializerBuildItem> deserializers) {
        serializers.produce(new JsonbSerializerBuildItem(VERTX_SERIALIZERS));
        deserializers.produce(new JsonbDeserializerBuildItem(VERTX_DESERIALIZERS));
    }

    @BuildStep
    public void beans(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonbMessageBodyReader.class.getName())
                .addBeanClass(JsonbMessageBodyWriter.class.getName())
                .setUnremovable().build());
    }

    public static void additionalProviders(BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
            BuildProducer<MessageBodyWriterBuildItem> additionalWriters, RuntimeType runtimeType) {

        additionalReaders.produce(
                new MessageBodyReaderBuildItem.Builder(JsonbMessageBodyReader.class.getName(), Object.class.getName())
                        .setMediaTypeStrings(Collections.singletonList(MediaType.APPLICATION_JSON))
                        .setBuiltin(true)
                        .setRuntimeType(runtimeType)
                        .build());
        additionalWriters.produce(
                new MessageBodyWriterBuildItem.Builder(JsonbMessageBodyWriter.class.getName(), Object.class.getName())
                        .setMediaTypeStrings(List.of(MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_NDJSON,
                                RestMediaType.APPLICATION_STREAM_JSON))
                        .setBuiltin(true)
                        .setRuntimeType(runtimeType)
                        .build());
    }
}
