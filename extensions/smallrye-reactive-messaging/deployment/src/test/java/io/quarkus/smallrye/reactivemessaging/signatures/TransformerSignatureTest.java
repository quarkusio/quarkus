package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;

@SuppressWarnings("unused")
public class TransformerSignatureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            BeanWithPublisherOfMessages.class,
                            BeanWithPublisherBuilderOfMessages.class,
                            BeanWithPublisherOfPayloads.class,
                            BeanWithPublisherBuilderOfPayloads.class,
                            Spy.class));

    @Inject
    BeanWithPublisherOfMessages beanWithPublisherOfMessages;
    @Inject
    BeanWithPublisherOfPayloads beanWithPublisherOfPayloads;
    @Inject
    BeanWithPublisherBuilderOfMessages beanWithPublisherBuilderOfMessages;
    @Inject
    BeanWithPublisherBuilderOfPayloads beanWithPublisherBuilderOfPayloads;

    @AfterAll
    public static void close() {
        Spy.executor.shutdown();
    }

    @Test
    public void test() {
        check(beanWithPublisherOfMessages);
        check(beanWithPublisherOfPayloads);
        check(beanWithPublisherBuilderOfMessages);
        check(beanWithPublisherBuilderOfPayloads);
    }

    private void check(Spy spy) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                spy.getEmitter().send(i);
            }
            spy.getEmitter().complete();
        }).start();

        await().until(() -> spy.items().size() == 10);
        assertThat(spy.items()).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @ApplicationScoped
    public static class BeanWithPublisherOfMessages extends Spy {

        @Inject
        @Channel("A")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("A")
        @Outgoing("AA")
        public Publisher<Message<String>> process(Publisher<Message<Integer>> publisher) {
            return ReactiveStreams.fromPublisher(publisher)
                    .flatMapCompletionStage(m -> CompletableFuture
                            .supplyAsync(() -> Message.of(Integer.toString(m.getPayload())), executor))
                    .buildRs();
        }

        @Incoming("AA")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherOfPayloads extends Spy {

        @Inject
        @Channel("B")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("B")
        @Outgoing("BB")
        public Publisher<String> process(Publisher<Integer> publisher) {
            return ReactiveStreams.fromPublisher(publisher)
                    .flatMapCompletionStage(i -> CompletableFuture.supplyAsync(() -> Integer.toString(i), executor))
                    .buildRs();
        }

        @Incoming("BB")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherBuilderOfMessages extends Spy {

        @Inject
        @Channel("C")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("C")
        @Outgoing("CC")
        public PublisherBuilder<Message<String>> process(PublisherBuilder<Message<Integer>> publisher) {
            return publisher
                    .flatMapCompletionStage(m -> CompletableFuture
                            .supplyAsync(() -> Message.of(Integer.toString(m.getPayload())), executor));
        }

        @Incoming("CC")
        public void consume(String item) {
            items().add(item);
        }

    }

    @ApplicationScoped
    public static class BeanWithPublisherBuilderOfPayloads extends Spy {

        @Inject
        @Channel("D")
        Emitter<Integer> emitter;

        public Emitter<Integer> getEmitter() {
            return emitter;
        }

        @Incoming("D")
        @Outgoing("DD")
        public PublisherBuilder<String> process(PublisherBuilder<Integer> publisher) {
            return publisher
                    .flatMapCompletionStage(i -> CompletableFuture.supplyAsync(() -> Integer.toString(i), executor));
        }

        @Incoming("DD")
        public void consume(String item) {
            items().add(item);
        }

    }

    public abstract static class Spy {
        List<String> items = new CopyOnWriteArrayList<>();
        static ExecutorService executor = Executors.newSingleThreadExecutor();

        public List<String> items() {
            return items;
        }

        public void close() {
            executor.shutdown();
        }

        abstract Emitter<Integer> getEmitter();

    }
}
