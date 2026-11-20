package io.quarkus.redis.lettuce.runtime.internal.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands.ReactiveRedisSubscriber;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

/**
 * The callbacks of a Pub/Sub subscriber run on the Vert.x context the subscription was made on, as with the Vert.x
 * Redis client, whose callbacks run on the context of the connection the subscriber opened: on that context itself
 * when it is a duplicated context (the one of a request being served, with its locals), on a new duplicated context
 * of it for every callback when it is a root context, and on duplicated contexts of one event loop context of its own
 * when the subscriber had none. In every case the callbacks run one at a time, in the order the messages arrive: on
 * one event loop, or on the worker threads of a subscriber on a worker context.
 */
class LettucePubSubVertxContextTest extends CommandsTestBase {

    private static final int MESSAGES = 50;
    private static final String LOCAL = "lettuce-pubsub-test-local";

    private final String channel = "channel-" + UUID.randomUUID();
    private final ReactivePubSubCommands<String> pubsub = reactiveDataSource().pubsub(String.class);
    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private final List<ReactiveRedisSubscriber> subscribers = new CopyOnWriteArrayList<>();

    /** Where a message was delivered: the context and the thread of the callback, and the local found there. */
    private record Delivery(String message, Context context, String thread, String local) {
    }

    @AfterEach
    void unsubscribe() {
        for (ReactiveRedisSubscriber subscriber : subscribers) {
            subscriber.unsubscribe().await().atMost(TIMEOUT);
        }
        Awaitility.await().untilAsserted(() -> assertThat(subscribedChannels()).isEmpty());
    }

    @Test
    void callbacksRunOnTheDuplicatedContextOfTheSubscriber() throws Exception {
        Context subscriberContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        subscribeOn(subscriberContext, () -> {
            ContextLocals.put(LOCAL, "of-the-subscriber");
            return pubsub.subscribe(channel, this::record);
        });

        publishMessages();

        assertThat(deliveries).extracting(Delivery::message).containsExactlyElementsOf(messages());
        assertThat(deliveries).allSatisfy(delivery -> {
            assertThat(delivery.context()).isSameAs(subscriberContext);
            assertThat(delivery.local()).isEqualTo("of-the-subscriber");
        });
    }

    @Test
    void aSubscriberOnARootContextGetsANewDuplicatedContextOfItForEveryCallback() throws Exception {
        Context root = vertx.getDelegate().getOrCreateContext();
        assertThat(VertxContext.isDuplicatedContext(root)).isFalse();
        subscribeOn(root, () -> pubsub.subscribe(channel, this::record));

        publishMessages();

        assertThat(deliveries).extracting(Delivery::message).containsExactlyElementsOf(messages());
        assertThat(deliveries).allSatisfy(delivery -> {
            assertThat(VertxContext.isDuplicatedContext(delivery.context())).isTrue();
            assertThat(VertxContext.getRootContext(delivery.context())).isSameAs(root);
        });
        // a context per callback: the locals of one callback are not seen by the next
        assertThat(deliveries).extracting(Delivery::context).doesNotHaveDuplicates();
        assertThat(deliveries).extracting(Delivery::thread).containsOnly(threadOf(root));
    }

    @Test
    void aSubscriberWithoutContextGetsOneEventLoopContextOfItsOwn() {
        // subscribed from the test thread, which has no Vert.x context
        subscribers.add(pubsub.subscribe(channel, this::record).await().atMost(TIMEOUT));

        publishMessages();

        assertThat(deliveries).extracting(Delivery::message).containsExactlyElementsOf(messages());
        assertThat(deliveries).allSatisfy(delivery -> assertThat(VertxContext.isDuplicatedContext(delivery.context()))
                .isTrue());
        Delivery first = deliveries.get(0);
        assertThat(deliveries).extracting(delivery -> VertxContext.getRootContext(delivery.context()))
                .containsOnly(VertxContext.getRootContext(first.context()));
        assertThat(deliveries).extracting(Delivery::context).doesNotHaveDuplicates();
        assertThat(first.thread()).contains("vert.x-eventloop");
        assertThat(deliveries).extracting(Delivery::thread).containsOnly(first.thread());
    }

    @Test
    void aSubscriberOnAWorkerContextGetsItsCallbacksOnWorkerThreadsInOrder() throws Exception {
        // a worker verticle, for instance: its callbacks may block, so they stay off the event loop, where the Vert.x
        // client would run them
        Context worker = ((VertxInternal) vertx.getDelegate()).createWorkerContext();
        subscribeOn(worker, () -> pubsub.subscribe(channel, this::record));

        publishMessages();

        assertThat(deliveries).extracting(Delivery::message).containsExactlyElementsOf(messages());
        assertThat(deliveries).allSatisfy(delivery -> {
            assertThat(VertxContext.isDuplicatedContext(delivery.context())).isTrue();
            assertThat(VertxContext.getRootContext(delivery.context())).isSameAs(worker);
            assertThat(delivery.thread()).contains("vert.x-worker-thread");
        });
    }

    @Test
    void onEndRunsOnTheContextOfTheSubscriber() throws Exception {
        Context subscriberContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        CompletableFuture<Context> ended = new CompletableFuture<>();
        ReactiveRedisSubscriber subscriber = subscribeOn(subscriberContext, () -> pubsub.subscribe(channel, message -> {
        }, () -> ended.complete(Vertx.currentContext()), failure -> {
        }));

        subscriber.unsubscribe().await().atMost(TIMEOUT);

        assertThat(ended.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isSameAs(subscriberContext);
    }

    @Test
    void aDecodingFailureIsReportedOnTheContextOfTheSubscriber() throws Exception {
        ReactivePubSubCommands<Person> people = reactiveDataSource().pubsub(Person.class);
        Context subscriberContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        CompletableFuture<Context> reported = new CompletableFuture<>();
        subscribeOn(subscriberContext, () -> people.subscribe(channel, person -> {
        }, () -> {
        }, failure -> reported.complete(Vertx.currentContext())));

        connection.sync().publish(channel.getBytes(StandardCharsets.UTF_8), "not a person".getBytes(StandardCharsets.UTF_8));

        assertThat(reported.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isSameAs(subscriberContext);
    }

    @Test
    void theItemsOfAStreamAreEmittedOnTheContextOfItsSubscriber() throws Exception {
        Context subscriberContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        List<Context> emittedOn = new CopyOnWriteArrayList<>();
        CompletableFuture<Cancellable> subscription = new CompletableFuture<>();
        subscriberContext.runOnContext(ignored -> subscription.complete(pubsub.subscribe(channel).subscribe()
                .with(item -> emittedOn.add(Vertx.currentContext()))));
        Cancellable stream = subscription.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        try {
            Awaitility.await().until(() -> subscribedChannels().size() == 1);

            for (int i = 0; i < 3; i++) {
                pubsub.publish(channel, "message-" + i).await().atMost(TIMEOUT);
            }
            Awaitility.await().until(() -> emittedOn.size() == 3);

            assertThat(emittedOn).containsOnly(subscriberContext);
        } finally {
            stream.cancel();
        }
    }

    /**
     * Makes the subscription {@code subscription} builds on {@code context}, as code running on that context would,
     * and unsubscribes it after the test.
     */
    private ReactiveRedisSubscriber subscribeOn(Context context, Supplier<Uni<ReactiveRedisSubscriber>> subscription)
            throws Exception {
        CompletableFuture<ReactiveRedisSubscriber> result = new CompletableFuture<>();
        context.runOnContext(ignored -> subscription.get().subscribe().with(result::complete,
                result::completeExceptionally));
        ReactiveRedisSubscriber subscriber = result.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        subscribers.add(subscriber);
        return subscriber;
    }

    private static String threadOf(Context context) throws Exception {
        CompletableFuture<String> thread = new CompletableFuture<>();
        context.runOnContext(ignored -> thread.complete(Thread.currentThread().getName()));
        return thread.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    /** The channel of this test, if the server has a subscriber on it. */
    private List<byte[]> subscribedChannels() {
        return connection.sync().pubsubChannels(channel.getBytes(StandardCharsets.UTF_8));
    }

    private void record(String message) {
        Context context = Vertx.currentContext();
        String local = VertxContext.isDuplicatedContext(context) ? ContextLocals.get(LOCAL, null) : null;
        deliveries.add(new Delivery(message, context, Thread.currentThread().getName(), local));
    }

    private static List<String> messages() {
        List<String> messages = new ArrayList<>(MESSAGES);
        for (int i = 0; i < MESSAGES; i++) {
            messages.add("message-" + i);
        }
        return messages;
    }

    private void publishMessages() {
        for (String message : messages()) {
            pubsub.publish(channel, message).await().atMost(TIMEOUT);
        }
        Awaitility.await().until(() -> deliveries.size() == MESSAGES);
    }

}
