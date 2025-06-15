package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.keyed.KeyValueExtractor;
import io.smallrye.reactive.messaging.keyed.KeyedMulti;

public class KeyedMultiInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MsgMetadata.class, ProcessorIngestingPayload.class,
                    MyKeyValueExtractor.class, Source.class, Sink.class));

    @Inject
    Sink sink;

    @Inject
    ProcessorIngestingPayload processor;

    @Test
    void test() {
        assertThat(sink.list()).allSatisfy(message -> {
            assertThat(message.getPayload()).containsAnyOf("key-0", "key-1");
        }).hasSize(10);

        assertThat(processor.called()).isEqualTo(2);
    }

    public static class MsgMetadata {

        private final String message;

        public MsgMetadata(String m) {
            this.message = m;
        }

        String getMessage() {
            return message;
        }

    }

    @ApplicationScoped
    public static class ProcessorIngestingPayload {

        private int called = 0;

        @Incoming("source")
        @Outgoing("sink")
        public Multi<String> process(KeyedMulti<String, String> multi) {
            called++;
            return multi.map(s -> multi.key() + "-" + s);
        }

        public int called() {
            return called;
        }
    }

    @ApplicationScoped
    public static class Source {

        @Outgoing("source")
        public Multi<Message<String>> source() {
            return Multi.createFrom().range(1, 11).map(
                    i -> Message.of(Integer.toString(i), Metadata.of(new MsgMetadata("key-" + (i % 2 == 0 ? 0 : 1)))));
        }

    }

    @ApplicationScoped
    public static class Sink {
        List<Message<String>> list = new ArrayList<>();

        @Incoming("sink")
        public CompletionStage<Void> consume(Message<String> message) {
            list.add(message);
            return message.ack();
        }

        public List<Message<String>> list() {
            return list;
        }
    }

    @ApplicationScoped
    public static class MyKeyValueExtractor implements KeyValueExtractor {

        @Override
        public boolean canExtract(Message<?> message, Type type, Type type1) {
            return message.getMetadata(MsgMetadata.class).isPresent();
        }

        @Override
        public Object extractKey(Message<?> message, Type type) {
            return message.getMetadata(MsgMetadata.class).get().message;
        }

        @Override
        public Object extractValue(Message<?> message, Type type) {
            return message.getPayload();
        }
    }
}
