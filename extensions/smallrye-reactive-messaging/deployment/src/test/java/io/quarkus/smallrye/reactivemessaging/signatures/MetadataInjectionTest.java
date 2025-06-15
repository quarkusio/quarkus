package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

public class MetadataInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MsgMetadata.class, CounterMetadata.class,
                    ProcessorIngestingPayload.class, Source.class, Sink.class));

    @Inject
    Sink sink;

    @Test
    void test() {
        assertThat(sink.list()).allSatisfy(message -> {
            CounterMetadata c = message.getMetadata(CounterMetadata.class)
                    .orElseThrow(() -> new AssertionError("Metadata expected"));
            MsgMetadata m = message.getMetadata(MsgMetadata.class)
                    .orElseThrow(() -> new AssertionError("Metadata expected"));
            assertThat(m.getMessage()).isEqualTo("foo");
            assertThat(c.getCount()).isNotEqualTo(0);
        }).hasSize(10);

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

    public static class CounterMetadata {

        private final int count;

        public CounterMetadata(int count) {
            this.count = count;
        }

        int getCount() {
            return count;
        }

    }

    @ApplicationScoped
    public static class ProcessorIngestingPayload {
        @Incoming("source")
        @Outgoing("intermediary")
        public String process(String p, MsgMetadata metadata) {
            assertThat(p).isNotNull();
            assertThat(metadata).isNotNull();
            assertThat(metadata.getMessage()).isEqualTo("foo");
            return p;
        }

        @Incoming("intermediary")
        @Outgoing("sink")
        public String process(String p, Optional<MsgMetadata> metadata, Optional<Locale> missing,
                Optional<List<String>> missingWithSubGeneric) {
            assertThat(p).isNotNull();
            assertThat(missing).isEmpty();
            assertThat(missingWithSubGeneric).isEmpty();
            assertThat(metadata).isPresent();
            assertThat(metadata.get().getMessage()).isEqualTo("foo");
            return p;
        }
    }

    @ApplicationScoped
    public static class Source {

        @Outgoing("source")
        public Multi<Message<String>> source() {
            return Multi.createFrom().range(1, 11).map(
                    i -> Message.of(Integer.toString(i), Metadata.of(new CounterMetadata(i), new MsgMetadata("foo"))));
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
}
