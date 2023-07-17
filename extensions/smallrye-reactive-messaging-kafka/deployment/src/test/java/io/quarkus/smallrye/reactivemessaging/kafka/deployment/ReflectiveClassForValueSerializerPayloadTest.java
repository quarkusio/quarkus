package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.groups.Tuple;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.Record;

class ReflectiveClassForValueSerializerPayloadTest {
    private static void doTest(Tuple[] expectations, Map<String, String> configs, Class<?>... classesToIndex) {
        Map<AnnotationInstance, Type> annotations = new HashMap<>();

        IndexView index = index(classesToIndex);
        Config config = configFromMap(configs);
        SmallRyeReactiveMessagingKafkaProcessor processor = new SmallRyeReactiveMessagingKafkaProcessor();
        processor.processOutgoingForReflectiveClassPayload(index, config, annotations::put);
        processor.processOutgoingChannelForReflectiveClassPayload(index, config, annotations::put);
        processor.processIncomingForReflectiveClassPayload(index, config, annotations::put);
        processor.processIncomingChannelForReflectiveClassPayload(index, config, annotations::put);

        assertThat(annotations)
                .extractingFromEntries(e -> e.getKey().value().asString(), e -> e.getValue().name().toString())
                .containsOnly(expectations);
    }

    static class JacksonDto {
    }

    private static class JacksonDtoSerializer extends ObjectMapperSerializer<JacksonDto> {
    }

    private static class JacksonDtoDeserializer extends ObjectMapperDeserializer<JacksonDto> {
        public JacksonDtoDeserializer() {
            super(JacksonDto.class);
        }
    }

    private static class JacksonDtoSerde {

        // Emitter @Channel
        @Channel("outgoing-channel1")
        Emitter<JacksonDto> emitter1;

        @Channel("outgoing-channel2")
        MutinyEmitter<JacksonDto> emitter2;

        @Channel("outgoing-channel3")
        MutinyEmitter<JacksonDto> emitter3;

        @Channel("outgoing-channel4")
        Publisher<JacksonDto> notExpected1;

        @Channel("outgoing-channel5")
        Emitter<JacksonDto> notExpected2;

        // @Outgoing
        @Outgoing("outgoing1")
        Publisher<Message<JacksonDto>> outgoing1() {
            return null;
        }

        @Outgoing("outgoing2")
        Message<JacksonDto> outgoing2() {
            return null;
        }

        @Outgoing("outgoing3")
        Publisher<JacksonDto> outgoing3() {
            return null;
        }

        @Outgoing("outgoing4")
        Multi<Record<String, JacksonDto>> outgoing4() {
            return null;
        }

        @Outgoing("outgoing5")
        JacksonDto outgoing5() {
            return null;
        }

        @Outgoing("outgoing6")
        JacksonDto notExpected() {
            return null;
        }

        // @Incoming
        @Incoming("incoming1")
        void incoming1(Message<JacksonDto> param) {
        }

        @Incoming("incoming2")
        void incoming2(Record<String, JacksonDto> param) {
        }

        @Incoming("incoming3")
        void incoming3(JacksonDto param) {
        }

        @Incoming("incoming4")
        void incoming4(ConsumerRecord<JacksonDto, String> param) {
        }

        @Incoming("incoming5")
        void notExpected(Publisher<JacksonDto> param) {
        }

        // Incoming @Channel
        @Channel("incoming-channel1")
        Multi<JacksonDto> incoming1;

        @Channel("incoming-channel2")
        Publisher<JacksonDto> incoming2;

        @Channel("incoming-channel3")
        Multi<Message<JacksonDto>> incoming3;

        @Channel("incoming-channel4")
        Publisher<Record<JacksonDto, String>> incoming4;

    }

    @Test
    void processAnnotationsForReflectiveClassJacksonPayload() {
        Tuple[] expectations = {
                tuple("outgoing-channel1", JacksonDto.class.getName()),
                tuple("outgoing-channel2", JacksonDto.class.getName()),
                tuple("outgoing-channel3", JacksonDto.class.getName()),
                tuple("outgoing1", JacksonDto.class.getName()),
                tuple("outgoing2", JacksonDto.class.getName()),
                tuple("outgoing3", JacksonDto.class.getName()),
                tuple("outgoing4", JacksonDto.class.getName()),
                tuple("outgoing5", JacksonDto.class.getName()),
                tuple("incoming1", JacksonDto.class.getName()),
                tuple("incoming2", JacksonDto.class.getName()),
                tuple("incoming3", JacksonDto.class.getName()),
                tuple("incoming4", JacksonDto.class.getName()),
                tuple("incoming-channel1", JacksonDto.class.getName()),
                tuple("incoming-channel2", JacksonDto.class.getName()),
                tuple("incoming-channel3", JacksonDto.class.getName()),
                tuple("incoming-channel4", JacksonDto.class.getName()),
        };
        Map<String, String> configMap = Map.ofEntries(
                Map.entry("mp.messaging.outgoing.outgoing-channel1.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel2.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel3.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel4.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing1.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing2.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing3.value.serializer", ObjectMapperSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing4.key.serializer", StringSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing4.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing5.value.serializer", JacksonDtoSerializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming1.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming2.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming3.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming4.key.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel1.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel2.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel3.value.deserializer", JacksonDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel4.key.deserializer", JacksonDtoDeserializer.class.getName()));

        doTest(expectations, configMap, JacksonDto.class, JacksonDtoSerializer.class, JacksonDtoDeserializer.class,
                JacksonDtoSerde.class);
    }

    static class JsonbDto {
    }

    static class JsonbDtoSerializer extends JsonbSerializer<JsonbDto> {
        @Override
        public byte[] serialize(String topic, Headers headers, JsonbDto data) {
            return null;
        }
    }

    static class JsonbDtoDeserializer extends JsonbDeserializer<JsonbDto> {

        public JsonbDtoDeserializer() {
            super(JsonbDto.class);
        }
    }

    private static class JsonbDtoSerde {
        // Emitter @Channel
        @Channel("outgoing-channel1")
        Emitter<Record<String, JsonbDto>> emitter1;

        @Channel("outgoing-channel2")
        MutinyEmitter<JsonbDto> emitter2;

        @Channel("outgoing-channel3")
        MutinyEmitter<KafkaRecord<JsonbDto, Long>> emitter3;

        @Channel("outgoing-channel4")
        Emitter<JsonbDto> emitter4;

        // @Outgoing
        @Outgoing("outgoing1")
        Publisher<Message<JsonbDto>> outgoing1() {
            return null;
        }

        @Outgoing("outgoing2")
        Message<JsonbDto> outgoing2() {
            return null;
        }

        @Outgoing("outgoing3")
        Publisher<JsonbDto> outgoing3() {
            return null;
        }

        @Outgoing("outgoing4")
        Multi<Record<String, JsonbDto>> outgoing4() {
            return null;
        }

        @Outgoing("outgoing5")
        JsonbDto outgoing5() {
            return null;
        }

        // @Incoming
        @Incoming("incoming1")
        void incoming1(Message<JsonbDto> param) {
        }

        @Incoming("incoming2")
        void incoming2(Record<String, JsonbDto> param) {
        }

        @Incoming("incoming3")
        void incoming3(JsonbDto param) {
        }

        @Incoming("incoming4")
        void incoming4(ConsumerRecord<JsonbDto, String> param) {
        }

        // Incoming @Channel
        @Channel("incoming-channel1")
        Multi<JsonbDto> incoming1;

        @Channel("incoming-channel2")
        Publisher<JsonbDto> incoming2;

        @Channel("incoming-channel3")
        Multi<Message<JsonbDto>> incoming3;

        @Channel("incoming-channel4")
        Publisher<Record<JsonbDto, String>> incoming4;

    }

    @Test
    void processAnnotationsForReflectiveClassJsonbPayload() {
        Tuple[] expectations = {
                tuple("outgoing-channel1", JsonbDto.class.getName()),
                tuple("outgoing-channel2", JsonbDto.class.getName()),
                tuple("outgoing-channel3", JsonbDto.class.getName()),
                tuple("outgoing-channel4", JsonbDto.class.getName()),
                tuple("outgoing1", JsonbDto.class.getName()),
                tuple("outgoing2", JsonbDto.class.getName()),
                tuple("outgoing3", JsonbDto.class.getName()),
                tuple("outgoing4", JsonbDto.class.getName()),
                tuple("outgoing5", JsonbDto.class.getName()),
                tuple("incoming1", JsonbDto.class.getName()),
                tuple("incoming2", JsonbDto.class.getName()),
                tuple("incoming3", JsonbDto.class.getName()),
                tuple("incoming4", JsonbDto.class.getName()),
                tuple("incoming-channel1", JsonbDto.class.getName()),
                tuple("incoming-channel2", JsonbDto.class.getName()),
                tuple("incoming-channel3", JsonbDto.class.getName()),
                tuple("incoming-channel4", JsonbDto.class.getName()),
        };

        Map<String, String> configMap = Map.ofEntries(
                Map.entry("mp.messaging.outgoing.outgoing-channel1.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel2.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel3.key.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing-channel4.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing1.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing2.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing3.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing4.value.serializer", JsonbDtoSerializer.class.getName()),
                Map.entry("mp.messaging.outgoing.outgoing5.value.serializer", JsonbDtoSerializer.class.getName()),

                Map.entry("mp.messaging.incoming.incoming1.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming2.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming3.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming4.key.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel1.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel2.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel3.value.deserializer", JsonbDtoDeserializer.class.getName()),
                Map.entry("mp.messaging.incoming.incoming-channel4.key.deserializer", JsonbDtoDeserializer.class.getName()));

        doTest(expectations, configMap, JsonbDto.class, JsonbDtoSerializer.class, JsonbDtoDeserializer.class,
                JsonbDtoSerde.class);
    }

    private static IndexView index(Class<?>... classes) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try {
                try (InputStream stream = DefaultSerdeConfigTest.class.getClassLoader()
                        .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                    indexer.index(stream);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return indexer.complete();
    }

    private static Config configFromMap(Map<String, String> configMap) {
        return new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("config-map", configMap) {
                })
                .build();
    }
}