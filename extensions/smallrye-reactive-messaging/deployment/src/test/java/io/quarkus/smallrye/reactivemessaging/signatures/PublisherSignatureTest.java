package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.quarkus.test.QuarkusUnitTest;

@SuppressWarnings("unused")
public class PublisherSignatureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanProducingAPublisherOfMessage.class,
                            BeanProducingAPublisherOfPayload.class,
                            BeanProducingAPublisherBuilderOfMessage.class,
                            BeanProducingAPublisherBuilderOfPayload.class,
                            BeanProducingPayloads.class,
                            BeanProducingMessages.class,
                            BeanProducingPayloadsAsynchronously.class,
                            BeanProducingMessagesAsynchronously.class,
                            Spy.class));

    @Inject
    BeanProducingAPublisherOfMessage beanProducingAPublisherOfMessage;
    @Inject
    BeanProducingAPublisherOfPayload beanProducingAPublisherOfPayload;
    @Inject
    BeanProducingAPublisherBuilderOfMessage beanProducingAPublisherBuilderOfMessage;
    @Inject
    BeanProducingAPublisherBuilderOfPayload beanProducingAPublisherBuilderOfPayload;
    @Inject
    BeanProducingPayloads beanProducingPayloads;
    @Inject
    BeanProducingMessages beanProducingMessages;
    @Inject
    BeanProducingPayloadsAsynchronously beanProducingPayloadsAsynchronously;
    @Inject
    BeanProducingMessagesAsynchronously beanProducingMessagesAsynchronously;

    @AfterEach
    public void closing() {
        beanProducingAPublisherOfMessage.close();
        beanProducingAPublisherOfPayload.close();
        beanProducingAPublisherBuilderOfMessage.close();
        beanProducingAPublisherBuilderOfPayload.close();
        beanProducingPayloads.close();
        beanProducingMessages.close();
        beanProducingPayloadsAsynchronously.close();
        beanProducingMessagesAsynchronously.close();
    }

    @Test
    public void test() {
        check(beanProducingAPublisherBuilderOfMessage);
        check(beanProducingAPublisherOfPayload);
        check(beanProducingAPublisherBuilderOfMessage);
        check(beanProducingAPublisherBuilderOfPayload);
        check(beanProducingPayloads);
        check(beanProducingMessages);
        check(beanProducingPayloadsAsynchronously);
        check(beanProducingMessagesAsynchronously);
    }

    private void check(Spy spy) {
        await().until(() -> spy.getItems().size() == 10);
        assertThat(spy.getItems()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfMessage extends Spy {

        @Outgoing("A")
        public Publisher<Message<Integer>> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .map(Message::of)
                    .buildRs();
        }

        @Incoming("A")
        public void consume(Integer item) {
            items.add(item);
        }

    }

    @ApplicationScoped
    public static class BeanProducingAPublisherOfPayload extends Spy {
        @Outgoing("B")
        public Publisher<Integer> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .buildRs();
        }

        @Incoming("B")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfMessage extends Spy {
        @Outgoing("C")
        public PublisherBuilder<Message<Integer>> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                    .map(Message::of);
        }

        @Incoming("C")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingAPublisherBuilderOfPayload extends Spy {
        @Outgoing("D")
        public PublisherBuilder<Integer> produce() {
            return ReactiveStreams.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        }

        @Incoming("D")
        public void consume(Integer item) {
            items.add(item);
        }
    }

    @ApplicationScoped
    public static class BeanProducingPayloads extends Spy {

        AtomicInteger count = new AtomicInteger();

        @Outgoing("E")
        public int produce() {
            return count.getAndIncrement();
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("E")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingMessages extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("F")
        public Message<Integer> produce() {
            return Message.of(count.getAndIncrement());
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("F")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingPayloadsAsynchronously extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("G")
        public CompletionStage<Integer> produce() {
            return CompletableFuture.supplyAsync(() -> count.getAndIncrement(), executor);
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("G")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    @ApplicationScoped
    public static class BeanProducingMessagesAsynchronously extends Spy {
        AtomicInteger count = new AtomicInteger();

        @Outgoing("H")
        public CompletionStage<Message<Integer>> produce() {
            return CompletableFuture.supplyAsync(() -> count.getAndIncrement(), executor).thenApply(Message::of);
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("H")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer integer) {
                    getItems().add(integer);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Ignored
                }

                @Override
                public void onComplete() {
                    // Ignored
                }
            };
        }
    }

    public static class Spy {
        List<Integer> items = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        public List<Integer> getItems() {
            return items;
        }

        public void close() {
            executor.shutdown();
        }

    }
}
