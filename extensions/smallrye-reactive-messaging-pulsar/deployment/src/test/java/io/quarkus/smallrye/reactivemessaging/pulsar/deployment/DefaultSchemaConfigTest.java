package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.apache.avro.specific.AvroGenerated;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.Schema;
import org.assertj.core.groups.Tuple;
import org.eclipse.microprofile.config.Config;
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
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.pulsar.SchemaProviderRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.smallrye.reactivemessaging.deployment.items.ConnectorManagedChannelBuildItem;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.GenericPayload;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.Targeted;
import io.smallrye.reactive.messaging.TargetedMessages;
import io.smallrye.reactive.messaging.pulsar.OutgoingMessage;
import io.smallrye.reactive.messaging.pulsar.PulsarBatchMessage;
import io.smallrye.reactive.messaging.pulsar.PulsarMessage;
import io.smallrye.reactive.messaging.pulsar.transactions.PulsarTransactions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultSchemaConfigTest {
    private static void doTest(Tuple[] expectations, Class<?>... classesToIndex) {
        doTest(null, expectations, Map.of(), classesToIndex);
    }

    private static void doTest(Tuple[] expectations, Map<String, String> generatedSchemas, Class<?>... classesToIndex) {
        doTest(null, expectations, generatedSchemas, classesToIndex);
    }

    private static void doTest(Config customConfig, Tuple[] expectations, Map<String, String> generatedSchemas,
            Class<?>... classesToIndex) {
        List<SyntheticBeanBuildItem> syntheticBeans = new ArrayList<>();
        List<RunTimeConfigurationDefaultBuildItem> configs = new ArrayList<>();

        List<Class<?>> classes = new ArrayList<>(Arrays.asList(classesToIndex));
        classes.add(Incoming.class);
        classes.add(Outgoing.class);
        DefaultSchemaDiscoveryState discovery = new DefaultSchemaDiscoveryState(index(classes)) {
            @Override
            Config getConfig() {
                return customConfig != null ? customConfig : super.getConfig();
            }

            @Override
            boolean isPulsarConnector(List<ConnectorManagedChannelBuildItem> list, boolean incoming, String channelName) {
                return true;
            }
        };
        RecorderContext rcMock = Mockito.mock(RecorderContext.class);
        Mockito.when(rcMock.classProxy(Mockito.anyString())).thenAnswer(a -> Class.forName(a.getArgument(0)));
        SyntheticBeanBuilder syntheticBean = new SyntheticBeanBuilder(syntheticBeans::add,
                new SchemaProviderRecorder(), rcMock) {
            @Override
            void produceSyntheticBeanSchema(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
                    RuntimeValue<?> runtimeValue, String schemaId, Type type) {
                // no-op
            }

            @Override
            void produceSyntheticBeanSchema(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItem,
                    Supplier<?> supplier, String schemaId, Type type) {
                // no-op
            }

            @Override
            String generateId(Type type, String targetType) {
                // remove the random bits
                return type.name().withoutPackagePrefix() + targetType + "Schema";
            }
        };

        try {
            new PulsarSchemaDiscoveryProcessor().discoverDefaultSerdeConfig(discovery,
                    Collections.emptyList(),
                    configs::add, syntheticBean);

            assertThat(configs)
                    .extracting(RunTimeConfigurationDefaultBuildItem::getKey, RunTimeConfigurationDefaultBuildItem::getValue)
                    .containsExactlyInAnyOrder(expectations);

            assertThat(syntheticBean.alreadyGeneratedSchema).containsExactlyInAnyOrderEntriesOf(generatedSchemas);
        } finally {
            // must not leak the lazily-initialized Config instance associated to the system classloader
            if (customConfig == null) {
                QuarkusConfigFactory.setConfig(null);
            }
        }
    }

    private static IndexView index(List<Class<?>> classes) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            final String resourceName = ClassLoaderHelper.fromClassNameToResourceName(clazz.getName());
            try {
                try (InputStream stream = DefaultSchemaConfigTest.class.getClassLoader()
                        .getResourceAsStream(resourceName)) {
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
                tuple("mp.messaging.outgoing.channel1.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel2.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel3.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel4.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel5.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel6.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel7.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel8.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel9.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel10.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel11.schema", "INT64"),
                tuple("mp.messaging.outgoing.channel12.schema", "INT64"),

                tuple("mp.messaging.incoming.channel13.schema", "STRING"),
                tuple("mp.messaging.incoming.channel14.schema", "STRING"),
                tuple("mp.messaging.incoming.channel15.schema", "STRING"),
                tuple("mp.messaging.incoming.channel16.schema", "STRING"),
                tuple("mp.messaging.incoming.channel17.schema", "STRING"),
                tuple("mp.messaging.incoming.channel18.schema", "STRING"),
                tuple("mp.messaging.incoming.channel19.schema", "STRING"),
                tuple("mp.messaging.incoming.channel20.schema", "STRING"),
                tuple("mp.messaging.incoming.channel21.schema", "STRING"),

                tuple("mp.messaging.incoming.channel22.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel23.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel24.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel25.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel26.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel27.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel28.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel29.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel30.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel31.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel32.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel33.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel34.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel35.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel36.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel37.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel38.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel39.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel40.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel41.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel42.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel43.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel44.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel45.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel46.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel47.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel48.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel49.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel50.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel51.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel52.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel53.schema",   "INT64"),

                tuple("mp.messaging.incoming.channel54.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel55.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel56.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel57.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel58.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel59.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel60.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel61.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel62.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel63.schema",   "INT64"),
                tuple("mp.messaging.incoming.channel64.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel65.schema",   "INT64"),
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
                tuple("mp.messaging.outgoing.channel1.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel2.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel3.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel4.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel5.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel6.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel7.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel8.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel9.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel10.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel11.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.outgoing.channel12.schema", "DefaultSchemaConfigTest$AvroDtoAVROSchema"),

                tuple("mp.messaging.incoming.channel13.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel14.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel15.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel16.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel17.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel18.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel19.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel20.schema", "BYTES"),
                tuple("mp.messaging.incoming.channel21.schema", "BYTES"),

                tuple("mp.messaging.incoming.channel22.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel23.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel24.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel25.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel26.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel27.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel28.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel29.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel30.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel31.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel32.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel33.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel34.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel35.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel36.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel37.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel38.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel39.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel40.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel41.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel42.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel43.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel44.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel45.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel46.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel47.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel48.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel49.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel50.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel51.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel52.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel53.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),

                tuple("mp.messaging.incoming.channel54.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel55.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel56.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel57.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel58.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel59.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel60.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel61.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel62.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel63.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
                tuple("mp.messaging.incoming.channel64.schema", "BYTES"),
                tuple("mp.messaging.outgoing.channel65.schema",   "DefaultSchemaConfigTest$AvroDtoAVROSchema"),
        };
        Map<String, String> generatedSchemas = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$AvroDto",
                "DefaultSchemaConfigTest$AvroDtoAVROSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, AvroDto.class, ByteArrayInAvroDtoOut.class);
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
    public void jacksonDtoInVertxJsonObjectOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel2.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel3.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel4.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel5.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel6.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel7.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel8.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel9.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel10.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel11.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.outgoing.channel12.schema", "JsonObjectJSON_OBJECTSchema"),

                tuple("mp.messaging.incoming.channel13.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel14.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel15.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel16.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel17.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel18.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel19.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel20.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel21.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),

                tuple("mp.messaging.incoming.channel22.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel23.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel24.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel25.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel26.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel27.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel28.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel29.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel30.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel31.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel32.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel33.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel34.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel35.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel36.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel37.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel38.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel39.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel40.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel41.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel42.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel43.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel44.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel45.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel46.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel47.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel48.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel49.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel50.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel51.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel52.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel53.schema",   "JsonObjectJSON_OBJECTSchema"),

                tuple("mp.messaging.incoming.channel54.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel55.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel56.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel57.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel58.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel59.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel60.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel61.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel62.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel63.schema",   "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel64.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel65.schema",   "JsonObjectJSON_OBJECTSchema"),
        };
        var generatedSchemas = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$JacksonDto",
                "DefaultSchemaConfigTest$JacksonDtoJSONSchema",
                "io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, JacksonDto.class, JacksonDtoInVertxJsonObjectOut.class);

    }

    static class JacksonDto {
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
    public void kafkaRecordIntUuidInRecordDoubleByteBufferOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel3.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel5.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel7.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel9.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel11.schema", "ByteBufferBYTE_BUFFERSchema"),

                tuple("mp.messaging.incoming.channel13.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel15.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel18.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel20.schema", "UUIDJSONSchema"),

                tuple("mp.messaging.incoming.channel22.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel23.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel26.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel27.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel30.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel31.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel34.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel35.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel38.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel39.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel42.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel43.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel46.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel47.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel50.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel51.schema",   "ByteBufferBYTE_BUFFERSchema"),

                tuple("mp.messaging.incoming.channel54.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel55.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel58.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel59.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel62.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel63.schema",   "ByteBufferBYTE_BUFFERSchema"),
        };
        var generatedSchemas = Map.of(
                "java.nio.ByteBuffer","ByteBufferBYTE_BUFFERSchema",
                "java.util.UUID","UUIDJSONSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, KafkaRecordIntUuidInRecordDoubleByteBufferOut.class);
    }

    private static class KafkaRecordIntUuidInRecordDoubleByteBufferOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method1() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method3() {
            return null;
        }

        @Outgoing("channel5")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method5() {
            return null;
        }

        @Outgoing("channel7")
        OutgoingMessage<java.nio.ByteBuffer> method7() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<OutgoingMessage<java.nio.ByteBuffer>> method9() {
            return null;
        }

        @Outgoing("channel11")
        Uni<OutgoingMessage<java.nio.ByteBuffer>> method11() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<PulsarMessage<java.util.UUID>> method13() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<PulsarMessage<java.util.UUID>, Void> method15() {
            return null;
        }

        @Incoming("channel18")
        CompletionStage<?> method18(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<PulsarMessage<java.util.UUID>, OutgoingMessage<java.nio.ByteBuffer>> method22() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<PulsarMessage<java.util.UUID>, OutgoingMessage<java.nio.ByteBuffer>> method24() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method26(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method28(PulsarMessage<UUID> msg) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method30(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        OutgoingMessage<java.nio.ByteBuffer> method32(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<OutgoingMessage<ByteBuffer>> method34(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<OutgoingMessage<java.nio.ByteBuffer>> method36(PulsarMessage<java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method38(Publisher<PulsarMessage<java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method40(
                PublisherBuilder<PulsarMessage<java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method42(Multi<PulsarMessage<java.util.UUID>> msg) {
            return null;
        }
    }

    // ---

    @Test
    public void consumerRecordIntUuidInProducerRecordDoubleByteBufferOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel3.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel5.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel7.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel9.schema",  "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.outgoing.channel11.schema", "ByteBufferBYTE_BUFFERSchema"),

                tuple("mp.messaging.incoming.channel13.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel15.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel18.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.incoming.channel20.schema", "UUIDJSONSchema"),

                tuple("mp.messaging.incoming.channel22.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel23.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel26.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel27.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel30.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel31.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel34.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel35.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel38.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel39.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel42.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel43.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel46.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel47.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel50.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel51.schema",   "ByteBufferBYTE_BUFFERSchema"),

                tuple("mp.messaging.incoming.channel54.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel55.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel58.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel59.schema",   "ByteBufferBYTE_BUFFERSchema"),
                tuple("mp.messaging.incoming.channel62.schema", "UUIDJSONSchema"),
                tuple("mp.messaging.outgoing.channel63.schema",   "ByteBufferBYTE_BUFFERSchema"),
        };
        var generatedSchemas = Map.of(
                "java.nio.ByteBuffer", "ByteBufferBYTE_BUFFERSchema",
                "java.util.UUID", "UUIDJSONSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, ConsumerRecordIntUuidInProducerRecordDoubleByteBufferOut.class);
    }

    private static class ConsumerRecordIntUuidInProducerRecordDoubleByteBufferOut {
        // @Outgoing

        @Outgoing("channel1")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method1() {
            return null;
        }

        @Outgoing("channel3")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method3() {
            return null;
        }

        @Outgoing("channel5")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method5() {
            return null;
        }

        @Outgoing("channel7")
        OutgoingMessage<java.nio.ByteBuffer> method7() {
            return null;
        }

        @Outgoing("channel9")
        CompletionStage<OutgoingMessage<java.nio.ByteBuffer>> method9() {
            return null;
        }

        @Outgoing("channel11")
        Uni<OutgoingMessage<java.nio.ByteBuffer>> method11() {
            return null;
        }

        // @Incoming

        @Incoming("channel13")
        Subscriber<org.apache.pulsar.client.api.Message<java.util.UUID>> method13() {
            return null;
        }

        @Incoming("channel15")
        SubscriberBuilder<org.apache.pulsar.client.api.Message<java.util.UUID>, Void> method15() {
            return null;
        }

        @Incoming("channel18")
        CompletionStage<?> method18(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel20")
        Uni<?> method20(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing

        @Incoming("channel22")
        @Outgoing("channel23")
        Processor<org.apache.pulsar.client.api.Message<java.util.UUID>, OutgoingMessage<java.nio.ByteBuffer>> method22() {
            return null;
        }

        @Incoming("channel26")
        @Outgoing("channel27")
        ProcessorBuilder<org.apache.pulsar.client.api.Message<java.util.UUID>, OutgoingMessage<java.nio.ByteBuffer>> method24() {
            return null;
        }

        @Incoming("channel30")
        @Outgoing("channel31")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method26(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel34")
        @Outgoing("channel35")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method28(
                org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel38")
        @Outgoing("channel39")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method30(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel42")
        @Outgoing("channel43")
        OutgoingMessage<java.nio.ByteBuffer> method32(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel46")
        @Outgoing("channel47")
        CompletionStage<OutgoingMessage<java.nio.ByteBuffer>> method34(
                org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        @Incoming("channel50")
        @Outgoing("channel51")
        Uni<OutgoingMessage<java.nio.ByteBuffer>> method36(org.apache.pulsar.client.api.Message<java.util.UUID> msg) {
            return null;
        }

        // @Incoming @Outgoing stream manipulation

        @Incoming("channel54")
        @Outgoing("channel55")
        Publisher<OutgoingMessage<java.nio.ByteBuffer>> method38(
                Publisher<org.apache.pulsar.client.api.Message<java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel58")
        @Outgoing("channel59")
        PublisherBuilder<OutgoingMessage<java.nio.ByteBuffer>> method40(
                PublisherBuilder<org.apache.pulsar.client.api.Message<java.util.UUID>> msg) {
            return null;
        }

        @Incoming("channel62")
        @Outgoing("channel63")
        Multi<OutgoingMessage<java.nio.ByteBuffer>> method42(Multi<org.apache.pulsar.client.api.Message<java.util.UUID>> msg) {
            return null;
        }
    }

    // ---

    @Test
    public void floatJsonArrayInShortByteArrayOut() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel2.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel3.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel4.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel5.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel6.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel7.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel8.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel9.schema",  "BYTES"),
                tuple("mp.messaging.outgoing.channel10.schema",  "BYTES"),

                tuple("mp.messaging.incoming.channel11.schema",  "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel12.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel13.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel14.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel15.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel16.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel17.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel18.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel19.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel20.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel21.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel22.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel23.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel24.schema", "JsonArrayJSON_ARRAYSchema"),
                tuple("mp.messaging.incoming.channel25.schema", "JsonArrayJSON_ARRAYSchema"),
        };
        var generatedSchemas = Map.of("io.vertx.core.json.JsonArray", "JsonArrayJSON_ARRAYSchema");
        // @formatter:on

        doTest(expectations, generatedSchemas, FloatJsonArrayInShortByteArrayOut.class);
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
        Emitter<PulsarMessage<byte[]>> emitter3;

        @Inject
        @Channel("channel4")
        Emitter<OutgoingMessage<byte[]>> emitter4;

        @Inject
        @Channel("channel5")
        Emitter<OutgoingMessage<byte[]>> emitter5;

        @Inject
        @Channel("channel6")
        MutinyEmitter<byte[]> emitter6;

        @Inject
        @Channel("channel7")
        MutinyEmitter<Message<byte[]>> emitter7;

        @Inject
        @Channel("channel8")
        MutinyEmitter<PulsarMessage<byte[]>> emitter8;

        @Inject
        @Channel("channel9")
        MutinyEmitter<OutgoingMessage<byte[]>> emitter9;

        @Inject
        @Channel("channel10")
        MutinyEmitter<OutgoingMessage<byte[]>> emitter10;

        // incoming

        @Inject
        @Channel("channel11")
        Publisher<JsonArray> consumer11;

        @Inject
        @Channel("channel12")
        Publisher<Message<JsonArray>> consumer12;

        @Inject
        @Channel("channel13")
        Publisher<PulsarMessage<JsonArray>> consumer13;

        @Inject
        @Channel("channel14")
        Publisher<OutgoingMessage<JsonArray>> consumer14;

        @Inject
        @Channel("channel15")
        Publisher<org.apache.pulsar.client.api.Message<JsonArray>> consumer15;

        @Inject
        @Channel("channel16")
        PublisherBuilder<JsonArray> consumer16;

        @Inject
        @Channel("channel17")
        PublisherBuilder<Message<JsonArray>> consumer17;

        @Inject
        @Channel("channel18")
        PublisherBuilder<PulsarMessage<JsonArray>> consumer18;

        @Inject
        @Channel("channel19")
        PublisherBuilder<OutgoingMessage<JsonArray>> consumer19;

        @Inject
        @Channel("channel20")
        PublisherBuilder<org.apache.pulsar.client.api.Message<JsonArray>> consumer20;

        @Inject
        @Channel("channel21")
        Multi<JsonArray> consumer21;

        @Inject
        @Channel("channel22")
        Multi<Message<JsonArray>> consumer22;

        @Inject
        @Channel("channel23")
        Multi<PulsarMessage<JsonArray>> consumer23;

        @Inject
        @Channel("channel24")
        Multi<OutgoingMessage<JsonArray>> consumer24;

        @Inject
        @Channel("channel25")
        Multi<org.apache.pulsar.client.api.Message<JsonArray>> consumer25;
    }

    @Test
    void produceDefaultConfigOnce() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel2.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.outgoing.channel4.schema", "INT32"),
        };
        var generatedSchemas = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$JacksonDto",
                "DefaultSchemaConfigTest$JacksonDtoJSONSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, JacksonDto.class, MultipleChannels.class);
    }

    private static class MultipleChannels {

        @Channel("channel1")
        Emitter<JacksonDto> emitter1;

        @Outgoing("channel1")
        Publisher<Message<JacksonDto>> method1() {
            return null;
        }

        @Outgoing("channel1")
        Publisher<JacksonDto> method1Duplicate() {
            return null;
        }

        @Channel("channel2")
        Multi<JacksonDto> channel2;

        @Incoming("channel2")
        void channel2Duplicate(JacksonDto jacksonDto) {
        }

        @Channel("channel3")
        Multi<OutgoingMessage<JacksonDto>> channel3;

        @Incoming("channel3")
        void channel3Duplicate(OutgoingMessage<JacksonDto> jacksonDto) {
        }

        @Channel("channel4")
        Emitter<OutgoingMessage<Integer>> emitterChannel4;

        @Outgoing("channel4")
        OutgoingMessage<Integer> method4() {
            return null;
        }
    }

    @Test
    void produceBatchConfigWithSerdeAutodetect() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel1.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel1.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel2.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel2.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel3.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel4.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel4.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel5.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel5.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel6.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel6.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel7.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel7.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel8.schema", "DefaultSchemaConfigTest$JacksonDtoJSONSchema"),
                tuple("mp.messaging.incoming.channel8.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel9.batchReceive", "true"),
                tuple("mp.messaging.incoming.channel10.batchReceive", "true"),
        };
        var generatedSchemas = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$JacksonDto",
                "DefaultSchemaConfigTest$JacksonDtoJSONSchema"
        );
        // @formatter:on

        doTest(expectations, generatedSchemas, JacksonDto.class, BatchChannels.class);
    }

    private static class BatchChannels {

        @Channel("channel1")
        Multi<List<JacksonDto>> channel1;

        @Incoming("channel2")
        void channel2(List<JacksonDto> jacksonDto) {
        }

        @Channel("channel3")
        Multi<PulsarBatchMessage<JacksonDto>> channel3;

        @Incoming("channel4")
        void channel4(PulsarBatchMessage<JacksonDto> jacksonDto) {
        }

        @Channel("channel5")
        Multi<Messages<JacksonDto>> channel5;

        @Incoming("channel6")
        void channel6(Messages<JacksonDto> jacksonDto) {
        }

        @Channel("channel7")
        Multi<Message<List<JacksonDto>>> channel7;

        @Incoming("channel8")
        void channel8(Message<List<JacksonDto>> jacksonDto) {
        }

        @Channel("channel9") // Not supported
        Multi<List<Message<JacksonDto>>> channel9;

        @Incoming("channel10") // Not supported
        void channel10(List<Message<JacksonDto>> jacksonDto) {
        }

    }

    // ---

    @Test
    public void connectorConfigNotOverridden() {
        // @formatter:off
        Tuple[] expectations1 = {
                // "mp.messaging.outgoing.channel1.schema" NOT expected, connector config exists
                // "mp.messaging.incoming.channel2.schema" NOT expected, connector config exists
                // "mp.messaging.incoming.channel3.schema" NOT expected, connector config exists
                // "mp.messaging.outgoing.channel4.schema" NOT expected, connector config exists
        };

        Tuple[] expectations2 = {
                // "mp.messaging.outgoing.channel1.schema" NOT expected, connector config exists
                // "mp.messaging.incoming.channel2.schema" NOT expected, connector config exists
                // "mp.messaging.incoming.channel3.schema" NOT expected, connector config exists
                // "mp.messaging.outgoing.channel4.schema" NOT expected, connector config exists
        };
        // @formatter:on

        doTest(new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "mp.messaging.connector.smallrye-pulsar.schema", "foo.Baz")) {
                })
                .build(), expectations1, Map.of(), ConnectorConfigNotOverriden.class);

        doTest(new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "mp.messaging.connector.smallrye-pulsar.schema", "foo.Baz")) {
                })
                .build(), expectations2, Map.of(), ConnectorConfigNotOverriden.class);
    }

    private static class ConnectorConfigNotOverriden {
        @Outgoing("channel1")
        OutgoingMessage<String> method1() {
            return null;
        }

        @Incoming("channel2")
        CompletionStage<?> method2(OutgoingMessage<String> msg) {
            return null;
        }

        @Incoming("channel3")
        @Outgoing("channel4")
        OutgoingMessage<String> method3(org.apache.pulsar.client.api.Message<String> msg) {
            return null;
        }
    }

    // ---

    @Test
    public void genericSerdeImplementationAutoDetect() {
        // @formatter:off

        Tuple[] expectations1 = {
                tuple("mp.messaging.outgoing.channel1.schema", "INT64"),
                tuple("mp.messaging.incoming.channel2.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$CustomDtoJSONSchema"),
        };
        var generatedSchemas1 = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto",
                "DefaultSchemaConfigTest$CustomDtoJSONSchema",
                "io.vertx.core.json.JsonObject","JsonObjectJSON_OBJECTSchema"
        );

        Tuple[] expectations2 = {
                tuple("mp.messaging.outgoing.channel1.schema", "INT64"),

                tuple("mp.messaging.incoming.channel2.schema", "JsonObjectJSON_OBJECTSchema"),

                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$CustomDtoJSONSchema"),
        };
        var generatedSchemas2 = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto",
                "DefaultSchemaConfigTest$CustomDtoJSONSchema",
                "io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema"
        );

        Tuple[] expectations3 = {
                tuple("mp.messaging.outgoing.channel1.schema", "INT64"),

                tuple("mp.messaging.incoming.channel2.schema", "JsonObjectJSON_OBJECTSchema"),

                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$CustomDtoJSONSchema"),
        };
        var generatedSchemas3 = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto",
                "DefaultSchemaConfigTest$CustomDtoJSONSchema",
                "io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema"
        );

        Tuple[] expectations4 = {
                tuple("mp.messaging.outgoing.channel1.schema", "INT64"),

                tuple("mp.messaging.incoming.channel2.schema", "JsonObjectJSON_OBJECTSchema"),

                tuple("mp.messaging.incoming.channel3.schema", "DefaultSchemaConfigTest$CustomDtoJSONSchema"),
        };
        var generatedSchemas4 = Map.of(
                "io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto",
                "DefaultSchemaConfigTest$CustomDtoJSONSchema",
                "io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema"
        );
        // @formatter:on

        doTest(expectations1, generatedSchemas1, CustomSerdeImplementation.class, CustomDto.class);

        doTest(expectations2, generatedSchemas2, CustomSerdeImplementation.class, CustomDto.class);

        doTest(expectations3, generatedSchemas3, CustomSerdeImplementation.class, CustomDto.class);

        doTest(expectations4, generatedSchemas4, CustomSerdeImplementation.class, CustomDto.class, CustomInterface.class);
    }

    private static class CustomDto {

    }

    private interface CustomInterface<T> {

    }

    private static class CustomSerdeImplementation {
        @Outgoing("channel1")
        Multi<Long> method1() {
            return null;
        }

        @Incoming("channel2")
        void method2(org.apache.pulsar.client.api.Message<JsonObject> msg) {

        }

        @Incoming("channel3")
        void method3(CustomDto payload) {

        }
    }

    @Test
    void pulsarTransactions() {
        // @formatter:off
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.tx.schema", "STRING"),
                tuple("mp.messaging.outgoing.tx.enableTransaction", "true"),
        };
        doTest(expectations, TransactionalProducer.class);

    }

    private static class TransactionalProducer {

        @Channel("tx")
        PulsarTransactions<String> pulsarTransactions;

    }

    @Test
    void repeatableIncomings() {
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel1.schema", "STRING"),
                tuple("mp.messaging.incoming.channel2.schema", "STRING"),
                tuple("mp.messaging.incoming.channel3.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel4.schema", "JsonObjectJSON_OBJECTSchema"),
        };
        var generatedSchemas = Map.of("io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema");
        doTest(expectations, generatedSchemas, RepeatableIncomingsChannels.class);
    }


    private static class RepeatableIncomingsChannels {

        @Incoming("channel1")
        @Incoming("channel2")
        void method1(String msg) {

        }

        @Incoming("channel3")
        @Incoming("channel4")
        void method2(JsonObject msg) {

        }

    }


    @Test
    void providedSchemaWithChannelName() {
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel1.schema", "STRING"),
                tuple("mp.messaging.incoming.channel3.schema", "JsonObjectJSON_OBJECTSchema"),
        };
        var generatedSchemas = Map.of("io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema");
        doTest(expectations, generatedSchemas, ProvidedSchemaWithChannelName.class, SchemaProvider.class);
    }


    private static class ProvidedSchemaWithChannelName {

        @Incoming("channel1")
        void method1(String msg) {

        }

        @Incoming("channel2")
        void method2(String msg) {

        }

        @Incoming("channel3")
        void method3(JsonObject msg) {

        }

        @Incoming("channel4")
        void method4(JsonObject msg) {

        }

    }

    private static class SchemaProvider {

        @Produces
        @Identifier("channel2")
        Schema<String> schema2 = Schema.STRING;

        @Produces
        @Identifier("channel4")
        Schema<JsonObject> schema4 = Schema.AVRO(JsonObject.class);

    }


    @Test
    void objectMapperSchema() {
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel2.schema", "STRING"),
        };
        var generatedSchemas = Map.of("io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto",
                "ObjectMapper<io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto>");
        doTest(new SmallRyeConfigBuilder()
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "mp.messaging.incoming.channel1.schema",
                        "ObjectMapper<io.quarkus.smallrye.reactivemessaging.pulsar.deployment.DefaultSchemaConfigTest$CustomDto>")) {
                })
                .build(), expectations, generatedSchemas, ObjectMapperSchema.class);
    }


    private static class ObjectMapperSchema {

        @Incoming("channel1")
        void method1(CustomDto msg) {

        }

        @Incoming("channel2")
        void method2(String msg) {

        }

    }

    @Test
    void targetedOutgoings() {
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel1.schema", "STRING"),
                tuple("mp.messaging.incoming.channel2.schema", "STRING"),
        };
        doTest(expectations, TargetedOutgoings.class);
    }


    private static class TargetedOutgoings {

        @Incoming("channel1")
        @Outgoing("out1")
        @Outgoing("out2")
        Targeted method1(String msg) {
            return null;
        }

        @Incoming("channel2")
        @Outgoing("out3")
        @Outgoing("out4")
        TargetedMessages method2(String msg) {
            return null;
        }

    }

    @Test
    void pulsarGenericPayload() {
        Tuple[] expectations = {
                tuple("mp.messaging.incoming.channel1.schema", "STRING"),
                tuple("mp.messaging.outgoing.out1.schema", "JsonObjectJSON_OBJECTSchema"),
                tuple("mp.messaging.incoming.channel2.schema", "STRING"),
                tuple("mp.messaging.outgoing.channel3.schema", "INT32"),
                tuple("mp.messaging.outgoing.channel4.schema", "INT64"),
        };
        var generatedSchemas = Map.of("io.vertx.core.json.JsonObject", "JsonObjectJSON_OBJECTSchema");
        doTest(expectations, generatedSchemas, GenericPayloadProducer.class);
    }

    private static class GenericPayloadProducer {
        @Incoming("channel1")
        @Outgoing("out1")
        GenericPayload<JsonObject> method1(String msg) {
            return null;
        }

        @Incoming("channel2")
        void method2(GenericPayload<String> msg) {
        }

        @Outgoing("channel3")
        GenericPayload<Integer> method3() {
            return null;
        }

        @Outgoing("channel4")
        Multi<GenericPayload<Long>> method4() {
            return null;
        }
    }

    @Test
    void instanceInjectionPoint() {
        Tuple[] expectations = {
                tuple("mp.messaging.outgoing.channel1.schema", "STRING"),
                tuple("mp.messaging.incoming.channel2.schema", "INT32"),
                tuple("mp.messaging.outgoing.channel3.schema", "DOUBLE"),
        };
        doTest(expectations, InstanceInjectionPoint.class);
    }

    private static class InstanceInjectionPoint {
        @Channel("channel1")
        Instance<Emitter<String>> emitter1;

        @Channel("channel2")
        Provider<Multi<Integer>> channel2;

        @Channel("channel3")
        InjectableInstance<MutinyEmitter<Double>> channel3;
    }


}
