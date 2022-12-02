package io.quarkus.smallrye.reactivemessaging.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;

@SuppressWarnings("unused")
public class SubscriberSignatureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanUsingSubscriberOfPayload.class,
                            BeanUsingSubscriberOfMessage.class,
                            BeanUsingConsumerMethod.class,
                            BeanConsumingMessages.class,
                            BeanConsumingPayloads.class,
                            Spy.class));

    @Inject
    BeanUsingSubscriberOfPayload beanUsingSubscriberOfPayload;
    @Inject
    BeanUsingSubscriberOfMessage beanUsingSubscriberOfMessage;
    @Inject
    BeanUsingConsumerMethod beanUsingConsumerMethod;
    @Inject
    BeanConsumingMessages beanConsumingMessages;
    @Inject
    BeanConsumingPayloads beanConsumingPayloads;

    @Test
    public void testMethodReturningASubscriberOfPayload() {
        Emitter<Integer> emitter = beanUsingSubscriberOfPayload.emitter();
        List<Integer> items = beanUsingSubscriberOfPayload.getItems();

        emit(emitter);

        await().until(() -> items.size() == 10);
        assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(beanUsingSubscriberOfPayload.hasCompleted()).isTrue();
    }

    private void emit(Emitter<Integer> emitter) {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                emitter.send(i);
            }
            emitter.complete();
        }).start();
    }

    @Test
    public void testMethodReturningASubscriberOfMessage() {
        Emitter<Integer> emitter = beanUsingSubscriberOfMessage.emitter();
        List<Integer> items = beanUsingSubscriberOfMessage.getItems();

        emit(emitter);

        await().until(() -> items.size() == 10);
        assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(beanUsingSubscriberOfMessage.getMessages()).hasSize(10);
        assertThat(beanUsingSubscriberOfPayload.hasCompleted()).isTrue();
    }

    @Test
    public void testMethodConsumingPayloadSynchronously() {
        Emitter<Integer> emitter = beanUsingConsumerMethod.emitter();
        List<Integer> items = beanUsingConsumerMethod.getItems();

        emit(emitter);

        await().until(() -> items.size() == 10);
        assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testMethodConsumingPayloadAsynchronously() {
        Emitter<Integer> emitter = beanConsumingPayloads.emitter();
        List<Integer> items = beanConsumingPayloads.getItems();

        emit(emitter);

        await().until(() -> items.size() == 10);
        assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testMethodConsumingMessages() {
        Emitter<Integer> emitter = beanConsumingMessages.emitter();
        List<Integer> items = beanConsumingMessages.getItems();

        emit(emitter);

        await().until(() -> items.size() == 10);
        assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(beanConsumingMessages.getMessages()).hasSize(10);
    }

    @ApplicationScoped
    public static class BeanUsingSubscriberOfPayload extends Spy {

        @Channel("A")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("A")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer o) {
                    items.add(o);
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }
            };
        }

    }

    @ApplicationScoped
    public static class BeanUsingSubscriberOfMessage extends Spy {

        @Channel("B")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("B")
        public Subscriber<Message<Integer>> consume() {
            return new Subscriber<Message<Integer>>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Message<Integer> o) {
                    messages.add(o);
                    items.add(o.getPayload());
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }
            };
        }

    }

    @ApplicationScoped
    public static class BeanUsingConsumerMethod extends Spy {

        @Channel("C")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("C")
        public void consume(Integer i) {
            items.add(i);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingMessages extends Spy {

        @Channel("D")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("D")
        public CompletionStage<Void> consume(Message<Integer> message) {
            getItems().add(message.getPayload());
            getMessages().add(message);
            return message.ack();
        }

    }

    @ApplicationScoped
    public static class BeanConsumingPayloads extends Spy {
        @Channel("E")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("E")
        public CompletionStage<Void> consume(Integer item) {
            getItems().add(item);
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class Spy {
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Integer> items = new CopyOnWriteArrayList<>();
        List<Message<Integer>> messages = new CopyOnWriteArrayList<>();

        public boolean hasCompleted() {
            return completed.get();
        }

        public List<Integer> getItems() {
            return items;
        }

        public List<Message<Integer>> getMessages() {
            return messages;
        }
    }

}
