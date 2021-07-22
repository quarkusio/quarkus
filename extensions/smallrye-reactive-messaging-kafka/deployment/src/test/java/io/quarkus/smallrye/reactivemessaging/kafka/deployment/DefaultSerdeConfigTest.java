package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.AvroGenerated;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.groups.Tuple;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.JsonArray;

public class DefaultSerdeConfigTest {
    private static void doTest(Tuple[] expectations, Class<?>... classesToIndex) {
        List<RunTimeConfigurationDefaultBuildItem> configs = new ArrayList<>();

        DefaultSerdeDiscoveryState discovery = new DefaultSerdeDiscoveryState(index(classesToIndex)) {
            @Override
            boolean isKafkaConnector(boolean incoming, String channelName) {
                return true;
            }
        };
        new SmallRyeReactiveMessagingKafkaProcessor().discoverDefaultSerdeConfig(discovery, configs::add);

        assertThat(configs)
                .extracting(RunTimeConfigurationDefaultBuildItem::getKey, RunTimeConfigurationDefaultBuildItem::getValue)
                .containsExactlyInAnyOrder(expectations);
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

    // ---

    @Test
    public void stringInLongOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.outgoing.channel12.value.serializer", "org.apache.kafka.common.serialization.LongSerializer"),

                tuple("mp.messaging.incoming.channel13.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),

                tuple("mp.messaging.incoming.channel22.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel25.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel28.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel29.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel32.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel33.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel36.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel37.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel40.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel41.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel44.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel45.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel48.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel49.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel52.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel53.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),

                tuple("mp.messaging.incoming.channel54.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel56.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel57.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel60.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel61.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
                tuple("mp.messaging.incoming.channel64.value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
                tuple("mp.messaging.outgoing.channel65.value.serializer",   "org.apache.kafka.common.serialization.LongSerializer"),
        };
        // @formatter:on

        doTest(expectations, StringInLongOut.class);
    }

    private static class StringInLongOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Message<Long>> method1() {
            return null;
        }

        @Outgoing("channel2")
        Publisher<Long> method2() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Message<Long>> method3() {
            return null;
        }

        @Outgoing("channel4")
        PublisherBuilder<Long> method4() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Message<Long>> method5() {
            return null;
        }

        @Outgoing("channel6")
        Multi<Long> method6() {
            return null;
        }

        @Outgoing("channel7")
        Message<Long> method7() {
            return null;
        }

        @Outgoing("channel8")
        Long method8() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Message<Long>> method9() {
            return null;
        }

        @Outgoing("channel10")
        CompletionStage<Long> method10() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Message<Long>> method11() {
            return null;
        }

        @Outgoing("channel12")
        Uni<Long> method12() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<Message<String>> method13() {
            return null;
        }

        @Incoming("channel14")
        Subscriber<String> method14() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<Message<String>, Void> method15() {
            return null;
        }

        @Incoming("channel16")
        SubscriberBuilder<String, Void> method16() {
            return null;
        }

        @Incoming("channel17")
        void method17(String msg) {
        }

        @Incoming("channel18")
        CompletionStage<?> method18(Message<String> msg) {
            return null;
        }

        @Incoming("channel19")
        CompletionStage<?> method19(String payload) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(Message<String> msg) {
            return null;
        }

        @Incoming("channel21")
        Uni<?> method21(String payload) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<Message<String>, Message<Long>> method22() {
            return null;
        }

        @Incoming("channel24")
        @Outgoing("channel25")
        Processor<String, Long> method23() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<Message<String>, Message<Long>> method24() {
            return null;
        }

        @Incoming("channel28")
        @Outgoing("channel29")
        ProcessorBuilder<String, Long> method25() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Message<Long>> method26(Message<String> msg) {
            return null;
        }

        @Incoming("channel32")
        @Outgoing("channel33")
        Publisher<Long> method27(String payload) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Message<Long>> method28(Message<String> msg) {
            return null;
        }

        @Incoming("channel36")
        @Outgoing("channel37")
        PublisherBuilder<Long> method29(String payload) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Message<Long>> method30(Message<String> msg) {
            return null;
        }

        @Incoming("channel40")
        @Outgoing("channel41")
        Multi<Long> method31(String payload) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Message<Long> method32(Message<String> msg) {
            return null;
        }

        @Incoming("channel44")
        @Outgoing("channel45")
        Long method33(String payload) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Message<Long>> method34(Message<String> msg) {
            return null;
        }

        @Incoming("channel48")
        @Outgoing("channel49")
        CompletionStage<Long> method35(String payload) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Message<Long>> method36(Message<String> msg) {
            return null;
        }

        @Incoming("channel52")
        @Outgoing("channel53")
        Uni<Long> method37(String payload) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Message<Long>> method38(Publisher<Message<String>> msg) {
            return null;
        }

        @Incoming("channel56")
        @Outgoing("channel57")
        Publisher<Long> method39(Publisher<String> payload) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Message<Long>> method40(PublisherBuilder<Message<String>> msg) {
            return null;
        }

        @Incoming("channel60")
        @Outgoing("channel61")
        PublisherBuilder<Long> method41(PublisherBuilder<String> payload) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Message<Long>> method42(Multi<Message<String>> msg) {
            return null;
        }

        @Incoming("channel64")
        @Outgoing("channel65")
        Multi<Long> method43(Multi<String> payload) {
            return null;
        }
    }

    // ---

    @Test
    public void byteArrayInAvroDtoOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel12.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),

                tuple("mp.messaging.incoming.channel13.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),

                tuple("mp.messaging.incoming.channel22.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel25.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel28.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel29.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel32.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel33.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel36.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel37.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel40.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel41.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel44.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel45.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel48.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel49.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel52.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel53.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),

                tuple("mp.messaging.incoming.channel54.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel56.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel57.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel60.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel61.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel64.value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer"),
                tuple("mp.messaging.outgoing.channel65.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
        };
        // @formatter:on

        doTest(expectations, AvroDto.class, ByteArrayInAvroDtoOut.class);
    }

    // simulating an Avro-generated class, the autodetection code only looks for this annotation
    @AvroGenerated
    private static class AvroDto {
    }

    private static class ByteArrayInAvroDtoOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Message<AvroDto>> method1() {
            return null;
        }

        @Outgoing("channel2")
        Publisher<AvroDto> method2() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Message<AvroDto>> method3() {
            return null;
        }

        @Outgoing("channel4")
        PublisherBuilder<AvroDto> method4() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Message<AvroDto>> method5() {
            return null;
        }

        @Outgoing("channel6")
        Multi<AvroDto> method6() {
            return null;
        }

        @Outgoing("channel7")
        Message<AvroDto> method7() {
            return null;
        }

        @Outgoing("channel8")
        AvroDto method8() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Message<AvroDto>> method9() {
            return null;
        }

        @Outgoing("channel10")
        CompletionStage<AvroDto> method10() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Message<AvroDto>> method11() {
            return null;
        }

        @Outgoing("channel12")
        Uni<AvroDto> method12() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<Message<byte[]>> method13() {
            return null;
        }

        @Incoming("channel14")
        Subscriber<byte[]> method14() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<Message<byte[]>, Void> method15() {
            return null;
        }

        @Incoming("channel16")
        SubscriberBuilder<byte[], Void> method16() {
            return null;
        }

        @Incoming("channel17")
        void method17(byte[] msg) {
        }

        @Incoming("channel18")
        CompletionStage<?> method18(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel19")
        CompletionStage<?> method19(byte[] payload) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel21")
        Uni<?> method21(byte[] payload) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<Message<byte[]>, Message<AvroDto>> method22() {
            return null;
        }

        @Incoming("channel24")
        @Outgoing("channel25")
        Processor<byte[], AvroDto> method23() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<Message<byte[]>, Message<AvroDto>> method24() {
            return null;
        }

        @Incoming("channel28")
        @Outgoing("channel29")
        ProcessorBuilder<byte[], AvroDto> method25() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Message<AvroDto>> method26(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel32")
        @Outgoing("channel33")
        Publisher<AvroDto> method27(byte[] payload) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Message<AvroDto>> method28(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel36")
        @Outgoing("channel37")
        PublisherBuilder<AvroDto> method29(byte[] payload) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Message<AvroDto>> method30(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel40")
        @Outgoing("channel41")
        Multi<AvroDto> method31(byte[] payload) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Message<AvroDto> method32(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel44")
        @Outgoing("channel45")
        AvroDto method33(byte[] payload) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Message<AvroDto>> method34(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel48")
        @Outgoing("channel49")
        CompletionStage<AvroDto> method35(byte[] payload) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Message<AvroDto>> method36(Message<byte[]> msg) {
            return null;
        }

        @Incoming("channel52")
        @Outgoing("channel53")
        Uni<AvroDto> method37(byte[] payload) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Message<AvroDto>> method38(Publisher<Message<byte[]>> msg) {
            return null;
        }

        @Incoming("channel56")
        @Outgoing("channel57")
        Publisher<AvroDto> method39(Publisher<byte[]> payload) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Message<AvroDto>> method40(PublisherBuilder<Message<byte[]>> msg) {
            return null;
        }

        @Incoming("channel60")
        @Outgoing("channel61")
        PublisherBuilder<AvroDto> method41(PublisherBuilder<byte[]> payload) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Message<AvroDto>> method42(Multi<Message<byte[]>> msg) {
            return null;
        }

        @Incoming("channel64")
        @Outgoing("channel65")
        Multi<AvroDto> method43(Multi<byte[]> payload) {
            return null;
        }
    }

    // ---

    @Test
    public void avroDtoInGenericRecordOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.outgoing.channel12.value.serializer", "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),

                tuple("mp.messaging.incoming.channel13.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel13.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel14.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel15.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel16.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel17.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel18.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel19.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel20.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel21.apicurio.registry.use-specific-avro-reader", "true"),

                tuple("mp.messaging.incoming.channel22.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel22.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel24.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel25.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel26.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel28.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel28.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel29.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel30.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel32.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel32.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel33.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel34.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel36.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel36.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel37.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel38.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel40.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel40.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel41.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel42.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel44.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel44.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel45.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel46.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel48.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel48.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel49.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel50.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel52.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel52.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel53.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),

                tuple("mp.messaging.incoming.channel54.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel54.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel56.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel56.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel57.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel58.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel60.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel60.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel61.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel62.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
                tuple("mp.messaging.incoming.channel64.value.deserializer", "io.apicurio.registry.serde.avro.AvroKafkaDeserializer"),
                tuple("mp.messaging.incoming.channel64.apicurio.registry.use-specific-avro-reader", "true"),
                tuple("mp.messaging.outgoing.channel65.value.serializer",   "io.apicurio.registry.serde.avro.AvroKafkaSerializer"),
        };
        // @formatter:on

        doTest(expectations, AvroDto.class, AvroDtoInGenericRecordOut.class);
    }

    private static class AvroDtoInGenericRecordOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Message<GenericRecord>> method1() {
            return null;
        }

        @Outgoing("channel2")
        Publisher<GenericRecord> method2() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Message<GenericRecord>> method3() {
            return null;
        }

        @Outgoing("channel4")
        PublisherBuilder<GenericRecord> method4() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Message<GenericRecord>> method5() {
            return null;
        }

        @Outgoing("channel6")
        Multi<GenericRecord> method6() {
            return null;
        }

        @Outgoing("channel7")
        Message<GenericRecord> method7() {
            return null;
        }

        @Outgoing("channel8")
        GenericRecord method8() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Message<GenericRecord>> method9() {
            return null;
        }

        @Outgoing("channel10")
        CompletionStage<GenericRecord> method10() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Message<GenericRecord>> method11() {
            return null;
        }

        @Outgoing("channel12")
        Uni<GenericRecord> method12() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<Message<AvroDto>> method13() {
            return null;
        }

        @Incoming("channel14")
        Subscriber<AvroDto> method14() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<Message<AvroDto>, Void> method15() {
            return null;
        }

        @Incoming("channel16")
        SubscriberBuilder<AvroDto, Void> method16() {
            return null;
        }

        @Incoming("channel17")
        void method17(AvroDto msg) {
        }

        @Incoming("channel18")
        CompletionStage<?> method18(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel19")
        CompletionStage<?> method19(AvroDto payload) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel21")
        Uni<?> method21(AvroDto payload) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<Message<AvroDto>, Message<GenericRecord>> method22() {
            return null;
        }

        @Incoming("channel24")
        @Outgoing("channel25")
        Processor<AvroDto, GenericRecord> method23() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<Message<AvroDto>, Message<GenericRecord>> method24() {
            return null;
        }

        @Incoming("channel28")
        @Outgoing("channel29")
        ProcessorBuilder<AvroDto, GenericRecord> method25() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Message<GenericRecord>> method26(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel32")
        @Outgoing("channel33")
        Publisher<GenericRecord> method27(AvroDto payload) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Message<GenericRecord>> method28(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel36")
        @Outgoing("channel37")
        PublisherBuilder<GenericRecord> method29(AvroDto payload) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Message<GenericRecord>> method30(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel40")
        @Outgoing("channel41")
        Multi<GenericRecord> method31(AvroDto payload) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Message<GenericRecord> method32(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel44")
        @Outgoing("channel45")
        GenericRecord method33(AvroDto payload) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Message<GenericRecord>> method34(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel48")
        @Outgoing("channel49")
        CompletionStage<GenericRecord> method35(AvroDto payload) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Message<GenericRecord>> method36(Message<AvroDto> msg) {
            return null;
        }

        @Incoming("channel52")
        @Outgoing("channel53")
        Uni<GenericRecord> method37(AvroDto payload) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Message<GenericRecord>> method38(Publisher<Message<AvroDto>> msg) {
            return null;
        }

        @Incoming("channel56")
        @Outgoing("channel57")
        Publisher<GenericRecord> method39(Publisher<AvroDto> payload) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Message<GenericRecord>> method40(PublisherBuilder<Message<AvroDto>> msg) {
            return null;
        }

        @Incoming("channel60")
        @Outgoing("channel61")
        PublisherBuilder<GenericRecord> method41(PublisherBuilder<AvroDto> payload) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Message<GenericRecord>> method42(Multi<Message<AvroDto>> msg) {
            return null;
        }

        @Incoming("channel64")
        @Outgoing("channel65")
        Multi<GenericRecord> method43(Multi<AvroDto> payload) {
            return null;
        }
    }

    // ---

    @Test
    public void jacksonDtoInVertxJsonObjectOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.outgoing.channel12.value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer"),

                tuple("mp.messaging.incoming.channel13.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),

                tuple("mp.messaging.incoming.channel22.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel25.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel28.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel29.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel32.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel33.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel36.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel37.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel40.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel41.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel44.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel45.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel48.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel49.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel52.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel53.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),

                tuple("mp.messaging.incoming.channel54.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel56.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel57.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel60.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel61.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
                tuple("mp.messaging.incoming.channel64.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel65.value.serializer",   "io.vertx.kafka.client.serialization.JsonObjectSerializer"),
        };
        // @formatter:on

        doTest(expectations, JacksonDto.class, JacksonDtoDeserializer.class, JacksonDtoInVertxJsonObjectOut.class);
    }

    static class JacksonDto {
    }

    static class JacksonDtoDeserializer extends ObjectMapperDeserializer<JacksonDto> {
        public JacksonDtoDeserializer() {
            super(JacksonDto.class);
        }

        @Override
        public JacksonDto deserialize(String topic, Headers headers, byte[] data) {
            return null;
        }
    }

    private static class JacksonDtoInVertxJsonObjectOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Message<io.vertx.core.json.JsonObject>> method1() {
            return null;
        }

        @Outgoing("channel2")
        Publisher<io.vertx.core.json.JsonObject> method2() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Message<io.vertx.core.json.JsonObject>> method3() {
            return null;
        }

        @Outgoing("channel4")
        PublisherBuilder<io.vertx.core.json.JsonObject> method4() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Message<io.vertx.core.json.JsonObject>> method5() {
            return null;
        }

        @Outgoing("channel6")
        Multi<io.vertx.core.json.JsonObject> method6() {
            return null;
        }

        @Outgoing("channel7")
        Message<io.vertx.core.json.JsonObject> method7() {
            return null;
        }

        @Outgoing("channel8")
        io.vertx.core.json.JsonObject method8() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Message<io.vertx.core.json.JsonObject>> method9() {
            return null;
        }

        @Outgoing("channel10")
        CompletionStage<io.vertx.core.json.JsonObject> method10() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Message<io.vertx.core.json.JsonObject>> method11() {
            return null;
        }

        @Outgoing("channel12")
        Uni<io.vertx.core.json.JsonObject> method12() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<Message<JacksonDto>> method13() {
            return null;
        }

        @Incoming("channel14")
        Subscriber<JacksonDto> method14() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<Message<JacksonDto>, Void> method15() {
            return null;
        }

        @Incoming("channel16")
        SubscriberBuilder<JacksonDto, Void> method16() {
            return null;
        }

        @Incoming("channel17")
        void method17(JacksonDto msg) {
        }

        @Incoming("channel18")
        CompletionStage<?> method18(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel19")
        CompletionStage<?> method19(JacksonDto payload) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel21")
        Uni<?> method21(JacksonDto payload) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<Message<JacksonDto>, Message<io.vertx.core.json.JsonObject>> method22() {
            return null;
        }

        @Incoming("channel24")
        @Outgoing("channel25")
        Processor<JacksonDto, io.vertx.core.json.JsonObject> method23() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<Message<JacksonDto>, Message<io.vertx.core.json.JsonObject>> method24() {
            return null;
        }

        @Incoming("channel28")
        @Outgoing("channel29")
        ProcessorBuilder<JacksonDto, io.vertx.core.json.JsonObject> method25() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Message<io.vertx.core.json.JsonObject>> method26(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel32")
        @Outgoing("channel33")
        Publisher<io.vertx.core.json.JsonObject> method27(JacksonDto payload) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Message<io.vertx.core.json.JsonObject>> method28(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel36")
        @Outgoing("channel37")
        PublisherBuilder<io.vertx.core.json.JsonObject> method29(JacksonDto payload) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Message<io.vertx.core.json.JsonObject>> method30(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel40")
        @Outgoing("channel41")
        Multi<io.vertx.core.json.JsonObject> method31(JacksonDto payload) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Message<io.vertx.core.json.JsonObject> method32(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel44")
        @Outgoing("channel45")
        io.vertx.core.json.JsonObject method33(JacksonDto payload) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Message<io.vertx.core.json.JsonObject>> method34(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel48")
        @Outgoing("channel49")
        CompletionStage<io.vertx.core.json.JsonObject> method35(JacksonDto payload) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Message<io.vertx.core.json.JsonObject>> method36(Message<JacksonDto> msg) {
            return null;
        }

        @Incoming("channel52")
        @Outgoing("channel53")
        Uni<io.vertx.core.json.JsonObject> method37(JacksonDto payload) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Message<io.vertx.core.json.JsonObject>> method38(Publisher<Message<JacksonDto>> msg) {
            return null;
        }

        @Incoming("channel56")
        @Outgoing("channel57")
        Publisher<io.vertx.core.json.JsonObject> method39(Publisher<JacksonDto> payload) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Message<io.vertx.core.json.JsonObject>> method40(PublisherBuilder<Message<JacksonDto>> msg) {
            return null;
        }

        @Incoming("channel60")
        @Outgoing("channel61")
        PublisherBuilder<io.vertx.core.json.JsonObject> method41(PublisherBuilder<JacksonDto> payload) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Message<io.vertx.core.json.JsonObject>> method42(Multi<Message<JacksonDto>> msg) {
            return null;
        }

        @Incoming("channel64")
        @Outgoing("channel65")
        Multi<io.vertx.core.json.JsonObject> method43(Multi<JacksonDto> payload) {
            return null;
        }
    }

    // ---

    @Test
    public void kafkaBytesInJsonbDtoOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.outgoing.channel12.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),

                tuple("mp.messaging.incoming.channel13.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),

                tuple("mp.messaging.incoming.channel22.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel25.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel28.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel29.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel32.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel33.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel36.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel37.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel40.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel41.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel44.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel45.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel48.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel49.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel52.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel53.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),

                tuple("mp.messaging.incoming.channel54.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel56.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel57.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel60.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel61.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel64.value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer"),
                tuple("mp.messaging.outgoing.channel65.value.serializer",   "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
        };
        // @formatter:on

        doTest(expectations, JsonbDto.class, JsonbDtoSerializer.class, KafkaBytesInJsonbDtoOut.class);
    }

    static class JsonbDto {
    }

    static class JsonbDtoSerializer extends JsonbSerializer<JsonbDto> {
        @Override
        public byte[] serialize(String topic, Headers headers, JsonbDto data) {
            return null;
        }
    }

    private static class KafkaBytesInJsonbDtoOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Message<JsonbDto>> method1() {
            return null;
        }

        @Outgoing("channel2")
        Publisher<JsonbDto> method2() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Message<JsonbDto>> method3() {
            return null;
        }

        @Outgoing("channel4")
        PublisherBuilder<JsonbDto> method4() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Message<JsonbDto>> method5() {
            return null;
        }

        @Outgoing("channel6")
        Multi<JsonbDto> method6() {
            return null;
        }

        @Outgoing("channel7")
        Message<JsonbDto> method7() {
            return null;
        }

        @Outgoing("channel8")
        JsonbDto method8() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Message<JsonbDto>> method9() {
            return null;
        }

        @Outgoing("channel10")
        CompletionStage<JsonbDto> method10() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Message<JsonbDto>> method11() {
            return null;
        }

        @Outgoing("channel12")
        Uni<JsonbDto> method12() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<Message<org.apache.kafka.common.utils.Bytes>> method13() {
            return null;
        }

        @Incoming("channel14")
        Subscriber<org.apache.kafka.common.utils.Bytes> method14() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<Message<org.apache.kafka.common.utils.Bytes>, Void> method15() {
            return null;
        }

        @Incoming("channel16")
        SubscriberBuilder<org.apache.kafka.common.utils.Bytes, Void> method16() {
            return null;
        }

        @Incoming("channel17")
        void method17(org.apache.kafka.common.utils.Bytes msg) {
        }

        @Incoming("channel18")
        CompletionStage<?> method18(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel19")
        CompletionStage<?> method19(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel21")
        Uni<?> method21(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<Message<org.apache.kafka.common.utils.Bytes>, Message<JsonbDto>> method22() {
            return null;
        }

        @Incoming("channel24")
        @Outgoing("channel25")
        Processor<org.apache.kafka.common.utils.Bytes, JsonbDto> method23() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<Message<org.apache.kafka.common.utils.Bytes>, Message<JsonbDto>> method24() {
            return null;
        }

        @Incoming("channel28")
        @Outgoing("channel29")
        ProcessorBuilder<org.apache.kafka.common.utils.Bytes, JsonbDto> method25() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Message<JsonbDto>> method26(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel32")
        @Outgoing("channel33")
        Publisher<JsonbDto> method27(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Message<JsonbDto>> method28(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel36")
        @Outgoing("channel37")
        PublisherBuilder<JsonbDto> method29(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Message<JsonbDto>> method30(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel40")
        @Outgoing("channel41")
        Multi<JsonbDto> method31(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Message<JsonbDto> method32(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel44")
        @Outgoing("channel45")
        JsonbDto method33(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Message<JsonbDto>> method34(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel48")
        @Outgoing("channel49")
        CompletionStage<JsonbDto> method35(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Message<JsonbDto>> method36(Message<org.apache.kafka.common.utils.Bytes> msg) {
            return null;
        }

        @Incoming("channel52")
        @Outgoing("channel53")
        Uni<JsonbDto> method37(org.apache.kafka.common.utils.Bytes payload) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Message<JsonbDto>> method38(Publisher<Message<org.apache.kafka.common.utils.Bytes>> msg) {
            return null;
        }

        @Incoming("channel56")
        @Outgoing("channel57")
        Publisher<JsonbDto> method39(Publisher<org.apache.kafka.common.utils.Bytes> payload) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Message<JsonbDto>> method40(PublisherBuilder<Message<org.apache.kafka.common.utils.Bytes>> msg) {
            return null;
        }

        @Incoming("channel60")
        @Outgoing("channel61")
        PublisherBuilder<JsonbDto> method41(PublisherBuilder<org.apache.kafka.common.utils.Bytes> payload) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Message<JsonbDto>> method42(Multi<Message<org.apache.kafka.common.utils.Bytes>> msg) {
            return null;
        }

        @Incoming("channel64")
        @Outgoing("channel65")
        Multi<JsonbDto> method43(Multi<org.apache.kafka.common.utils.Bytes> payload) {
            return null;
        }
    }

    // ---

    @Test
    public void kafkaRecordIntUuidInRecordDoubleByteBufferOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel1.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel3.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel5.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel7.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel9.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel11.key.serializer",   "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "org.apache.kafka.common.serialization.ByteBufferSerializer"),

                tuple("mp.messaging.incoming.channel13.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel13.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel15.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel18.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel20.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),

                tuple("mp.messaging.incoming.channel22.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel22.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel23.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel26.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel27.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel30.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel31.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel34.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel35.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel38.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel39.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel42.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel43.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel46.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel47.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel50.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel51.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),

                tuple("mp.messaging.incoming.channel54.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel54.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel55.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel58.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel59.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel62.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel63.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
        };
        // @formatter:on

        doTest(expectations, KafkaRecordIntUuidInRecordDoubleByteBufferOut.class);
    }

    private static class KafkaRecordIntUuidInRecordDoubleByteBufferOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<Record<Double, java.nio.ByteBuffer>> method1() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<Record<Double, java.nio.ByteBuffer>> method3() {
            return null;
        }

        @Outgoing("channel5")
        Multi<Record<Double, java.nio.ByteBuffer>> method5() {
            return null;
        }

        @Outgoing("channel7")
        Record<Double, java.nio.ByteBuffer> method7() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<Record<Double, java.nio.ByteBuffer>> method9() {
            return null;
        }

        @Outgoing("channel11")
        Uni<Record<Double, java.nio.ByteBuffer>> method11() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<KafkaRecord<Integer, java.util.UUID>> method13() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<KafkaRecord<Integer, java.util.UUID>, Void> method15() {
            return null;
        }

        @Incoming("channel18")
        CompletionStage<?> method18(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<KafkaRecord<Integer, java.util.UUID>, Record<Double, java.nio.ByteBuffer>> method22() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<KafkaRecord<Integer, java.util.UUID>, Record<Double, java.nio.ByteBuffer>> method24() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<Record<Double, java.nio.ByteBuffer>> method26(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<Record<Double, java.nio.ByteBuffer>> method28(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<Record<Double, java.nio.ByteBuffer>> method30(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        Record<Double, java.nio.ByteBuffer> method32(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<Record<Double, java.nio.ByteBuffer>> method34(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<Record<Double, java.nio.ByteBuffer>> method36(KafkaRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<Record<Double, java.nio.ByteBuffer>> method38(Publisher<KafkaRecord<Integer, java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<Record<Double, java.nio.ByteBuffer>> method40(
                PublisherBuilder<KafkaRecord<Integer, java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<Record<Double, java.nio.ByteBuffer>> method42(Multi<KafkaRecord<Integer, java.util.UUID>> msg) {
            return null;
        }
    }

    // ---

    @Test
    public void consumerRecordIntUuidInProducerRecordDoubleByteBufferOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel1.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel3.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel5.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel7.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel9.key.serializer",    "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer",  "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.outgoing.channel11.key.serializer",   "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel11.value.serializer", "org.apache.kafka.common.serialization.ByteBufferSerializer"),

                tuple("mp.messaging.incoming.channel13.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel13.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel15.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel18.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.incoming.channel20.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),

                tuple("mp.messaging.incoming.channel22.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel22.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel23.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel23.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel26.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel26.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel27.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel27.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel30.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel30.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel31.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel31.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel34.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel34.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel35.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel35.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel38.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel38.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel39.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel39.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel42.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel42.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel43.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel43.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel46.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel46.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel47.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel47.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel50.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel50.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel51.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel51.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),

                tuple("mp.messaging.incoming.channel54.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel54.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel55.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel55.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel58.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel58.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel59.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel59.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
                tuple("mp.messaging.incoming.channel62.key.deserializer",   "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel62.value.deserializer", "org.apache.kafka.common.serialization.UUIDDeserializer"),
                tuple("mp.messaging.outgoing.channel63.key.serializer",     "org.apache.kafka.common.serialization.DoubleSerializer"),
                tuple("mp.messaging.outgoing.channel63.value.serializer",   "org.apache.kafka.common.serialization.ByteBufferSerializer"),
        };
        // @formatter:on

        doTest(expectations, ConsumerRecordIntUuidInProducerRecordDoubleByteBufferOut.class);
    }

    private static class ConsumerRecordIntUuidInProducerRecordDoubleByteBufferOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<ProducerRecord<Double, java.nio.ByteBuffer>> method1() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<ProducerRecord<Double, java.nio.ByteBuffer>> method3() {
            return null;
        }

        @Outgoing("channel5")
        Multi<ProducerRecord<Double, java.nio.ByteBuffer>> method5() {
            return null;
        }

        @Outgoing("channel7")
        ProducerRecord<Double, java.nio.ByteBuffer> method7() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<ProducerRecord<Double, java.nio.ByteBuffer>> method9() {
            return null;
        }

        @Outgoing("channel11")
        Uni<ProducerRecord<Double, java.nio.ByteBuffer>> method11() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<ConsumerRecord<Integer, java.util.UUID>> method13() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<ConsumerRecord<Integer, java.util.UUID>, Void> method15() {
            return null;
        }

        @Incoming("channel18")
        CompletionStage<?> method18(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<ConsumerRecord<Integer, java.util.UUID>, ProducerRecord<Double, java.nio.ByteBuffer>> method22() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<ConsumerRecord<Integer, java.util.UUID>, ProducerRecord<Double, java.nio.ByteBuffer>> method24() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<ProducerRecord<Double, java.nio.ByteBuffer>> method26(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<ProducerRecord<Double, java.nio.ByteBuffer>> method28(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<ProducerRecord<Double, java.nio.ByteBuffer>> method30(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        ProducerRecord<Double, java.nio.ByteBuffer> method32(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<ProducerRecord<Double, java.nio.ByteBuffer>> method34(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<ProducerRecord<Double, java.nio.ByteBuffer>> method36(ConsumerRecord<Integer, java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<ProducerRecord<Double, java.nio.ByteBuffer>> method38(
                Publisher<ConsumerRecord<Integer, java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<ProducerRecord<Double, java.nio.ByteBuffer>> method40(
                PublisherBuilder<ConsumerRecord<Integer, java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<ProducerRecord<Double, java.nio.ByteBuffer>> method42(Multi<ConsumerRecord<Integer, java.util.UUID>> msg) {
            return null;
        }
    }

    // ---

    @Test
    public void floatJsonArrayInShortByteArrayOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel2.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel3.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel3.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel4.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel5.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel5.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel6.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel7.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel8.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel8.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel9.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel9.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),
                tuple("mp.messaging.outgoing.channel10.key.serializer",    "org.apache.kafka.common.serialization.ShortSerializer"),
                tuple("mp.messaging.outgoing.channel10.value.serializer",  "org.apache.kafka.common.serialization.ByteArraySerializer"),

                tuple("mp.messaging.incoming.channel11.value.deserializer",  "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel12.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel13.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel13.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel14.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel14.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel15.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel15.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel16.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel17.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel18.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel18.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel19.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel19.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel20.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel20.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel21.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel22.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel23.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel23.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel24.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel24.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
                tuple("mp.messaging.incoming.channel25.key.deserializer",   "org.apache.kafka.common.serialization.FloatDeserializer"),
                tuple("mp.messaging.incoming.channel25.value.deserializer", "io.vertx.kafka.client.serialization.JsonArrayDeserializer"),
        };
        // @formatter:on

        doTest(expectations, FloatJsonArrayInShortByteArrayOut.class);
    }

    private static class FloatJsonArrayInShortByteArrayOut {
        // outgoing

        @Inject
        @Channel("channel1")
        Emitter<byte[]> emitter1;

        @Inject
        @Channel("channel2")
        Emitter<Message<byte[]>> emitter2;

        @Inject
        @Channel("channel3")
        Emitter<KafkaRecord<Short, byte[]>> emitter3;

        @Inject
        @Channel("channel4")
        Emitter<Record<Short, byte[]>> emitter4;

        @Inject
        @Channel("channel5")
        Emitter<ProducerRecord<Short, byte[]>> emitter5;

        @Inject
        @Channel("channel6")
        MutinyEmitter<byte[]> emitter6;

        @Inject
        @Channel("channel7")
        MutinyEmitter<Message<byte[]>> emitter7;

        @Inject
        @Channel("channel8")
        MutinyEmitter<KafkaRecord<Short, byte[]>> emitter8;

        @Inject
        @Channel("channel9")
        MutinyEmitter<Record<Short, byte[]>> emitter9;

        @Inject
        @Channel("channel10")
        MutinyEmitter<ProducerRecord<Short, byte[]>> emitter10;

        // incoming

        @Inject
        @Channel("channel11")
        Publisher<JsonArray> consumer11;

        @Inject
        @Channel("channel12")
        Publisher<Message<JsonArray>> consumer12;

        @Inject
        @Channel("channel13")
        Publisher<KafkaRecord<Float, JsonArray>> consumer13;

        @Inject
        @Channel("channel14")
        Publisher<Record<Float, JsonArray>> consumer14;

        @Inject
        @Channel("channel15")
        Publisher<ConsumerRecord<Float, JsonArray>> consumer15;

        @Inject
        @Channel("channel16")
        PublisherBuilder<JsonArray> consumer16;

        @Inject
        @Channel("channel17")
        PublisherBuilder<Message<JsonArray>> consumer17;

        @Inject
        @Channel("channel18")
        PublisherBuilder<KafkaRecord<Float, JsonArray>> consumer18;

        @Inject
        @Channel("channel19")
        PublisherBuilder<Record<Float, JsonArray>> consumer19;

        @Inject
        @Channel("channel20")
        PublisherBuilder<ConsumerRecord<Float, JsonArray>> consumer20;

        @Inject
        @Channel("channel21")
        Multi<JsonArray> consumer21;

        @Inject
        @Channel("channel22")
        Multi<Message<JsonArray>> consumer22;

        @Inject
        @Channel("channel23")
        Multi<KafkaRecord<Float, JsonArray>> consumer23;

        @Inject
        @Channel("channel24")
        Multi<Record<Float, JsonArray>> consumer24;

        @Inject
        @Channel("channel25")
        Multi<ConsumerRecord<Float, JsonArray>> consumer25;
    }

    @Test
    void produceDefaultConfigOnce() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.value.serializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JsonbDtoSerializer"),
                tuple("mp.messaging.incoming.channel2.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.incoming.channel3.key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer"),
                tuple("mp.messaging.incoming.channel3.value.deserializer", "io.quarkus.smallrye.reactivemessaging.kafka.deployment.DefaultSerdeConfigTest$JacksonDtoDeserializer"),
                tuple("mp.messaging.outgoing.channel4.key.serializer", "org.apache.kafka.common.serialization.StringSerializer"),
                tuple("mp.messaging.outgoing.channel4.value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer"),
        };
        // @formatter:on

        doTest(expectations, JsonbDto.class, JsonbDtoSerializer.class, JacksonDto.class, JacksonDtoDeserializer.class,
                MultipleChannels.class);
    }

    private static class MultipleChannels {

        @Channel("channel1")
        Emitter<JsonbDto> emitter1;

        @Outgoing("channel1")
        Publisher<Message<JsonbDto>> method1() {
            return null;
        }

        @Outgoing("channel1")
        Publisher<JsonbDto> method1Duplicate() {
            return null;
        }

        @Channel("channel2")
        Multi<JacksonDto> channel2;

        @Incoming("channel2")
        void channel2Duplicate(JacksonDto jacksonDto) {
        }

        @Channel("channel3")
        Multi<Record<Integer, JacksonDto>> channel3;

        @Incoming("channel3")
        void channel3Duplicate(Record<Integer, JacksonDto> jacksonDto) {
        }

        @Channel("channel4")
        Emitter<ProducerRecord<String, Integer>> emitterChannel4;

        @Outgoing("channel4")
        ProducerRecord<String, Integer> method4() {
            return null;
        }
    }
}
