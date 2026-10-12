package io.quarkus.redis.lettuce.runtime.internal.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.KillArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands.ReactiveRedisSubscriber;
import io.quarkus.redis.datasource.pubsub.RedisPubSubMessage;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.DefaultRedisPubSubMessage;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

class LettucePubSubCommandsTest extends CommandsTestBase {

    final String channel = "channel-" + UUID.randomUUID();
    final Person luke = new Person("luke", "skywalker");
    final Person leia = new Person("leia", "skywalker");

    ReactiveRedisDataSource reactiveDs;
    ReactivePubSubCommands<Person> pubsub;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        pubsub = reactiveDs.pubsub(Person.class);
    }

    @AfterEach
    void noChannelLeftSubscribed() {
        Awaitility.await().untilAsserted(() -> assertThat(connection.sync().pubsubChannels()).isEmpty());
    }

    @Test
    void getDataSource() {
        assertThat(pubsub.getDataSource()).isSameAs(reactiveDs);
    }

    @Test
    void publishWithoutSubscriberCompletes() {
        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
    }

    @Test
    void subscribeReceivesPublishedMessagesAndUnsubscribeClosesItsConnection() {
        long before = connectionCount();
        List<Person> people = new CopyOnWriteArrayList<>();
        List<Boolean> onDuplicatedContext = new CopyOnWriteArrayList<>();

        ReactiveRedisSubscriber subscriber = pubsub.subscribe(channel, person -> {
            onDuplicatedContext.add(VertxContext.isOnDuplicatedContext());
            people.add(person);
        }).await().atMost(TIMEOUT);
        // Each subscriber owns a dedicated Pub/Sub connection, apart from the shared one and the pool.
        assertThat(connectionCount()).isEqualTo(before + 1);
        assertThat(connection.sync().pubsubChannels()).hasSize(1);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 1);
        pubsub.publish(channel, leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 2);

        assertThat(people).containsExactly(luke, leia);
        assertThat(onDuplicatedContext).containsOnly(true);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void messagesOnOtherChannelsAreNotDelivered() {
        List<Person> people = new CopyOnWriteArrayList<>();
        ReactiveRedisSubscriber subscriber = pubsub.subscribe(channel, people::add).await().atMost(TIMEOUT);

        pubsub.publish(channel + "-other", luke).await().atMost(TIMEOUT);
        pubsub.publish(channel, leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 1);
        assertThat(people).containsExactly(leia);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
    }

    @Test
    void connectionIsClosedAndOnEndCalledOnlyAfterTheLastChannelIsUnsubscribed() {
        long before = connectionCount();
        String second = channel + "-second";
        List<String> receivedOn = new CopyOnWriteArrayList<>();
        AtomicInteger ended = new AtomicInteger();

        ReactiveRedisSubscriber subscriber = pubsub
                .subscribe(List.of(channel, second), (name, person) -> receivedOn.add(name), ended::incrementAndGet, null)
                .await().atMost(TIMEOUT);
        assertThat(connectionCount()).isEqualTo(before + 1);

        subscriber.unsubscribe(channel).await().atMost(TIMEOUT);
        assertThat(connectionCount()).isEqualTo(before + 1);
        assertThat(ended).hasValue(0);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
        pubsub.publish(second, leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> receivedOn.size() == 1);
        assertThat(receivedOn).containsExactly(second);

        subscriber.unsubscribe(second).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> connectionCount() == before);
        Awaitility.await().until(() -> ended.get() == 1);
    }

    @Test
    void invalidArgumentsAreRejectedEagerly() {
        assertThatThrownBy(() -> pubsub.publish(null, luke)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.publish(channel, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe((String) null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe(channel, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe(List.of(" "), (c, p) -> {
        }, null, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");
        assertThatThrownBy(() -> pubsub.subscribe(List.of(), (c, p) -> {
        }, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void channelAwareCallbackReceivesTheChannelName() {
        List<RedisPubSubMessage<Person>> messages = new CopyOnWriteArrayList<>();
        ReactiveRedisSubscriber subscriber = pubsub
                .subscribe(List.of(channel), (name, person) -> messages.add(new DefaultRedisPubSubMessage<>(person, name)))
                .await().atMost(TIMEOUT);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> messages.size() == 1);
        assertThat(messages.get(0).getChannel()).isEqualTo(channel);
        assertThat(messages.get(0).getPayload()).isEqualTo(luke);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
    }

    @Test
    void patternSubscriptionReceivesMatchingChannelsAndReportsTheChannelName() {
        long before = connectionCount();
        List<String> channels = new CopyOnWriteArrayList<>();
        List<Person> people = new CopyOnWriteArrayList<>();
        List<Boolean> onDuplicatedContext = new CopyOnWriteArrayList<>();

        ReactiveRedisSubscriber subscriber = pubsub.subscribeToPattern(channel + "-*", (name, person) -> {
            onDuplicatedContext.add(VertxContext.isOnDuplicatedContext());
            channels.add(name);
            people.add(person);
        }).await().atMost(TIMEOUT);
        assertThat(connectionCount()).isEqualTo(before + 1);
        assertThat(connection.sync().pubsubNumpat()).isEqualTo(1);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT); // does not match the pattern
        pubsub.publish(channel + "-a", luke).await().atMost(TIMEOUT);
        pubsub.publish(channel + "-b", leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 2);

        assertThat(people).containsExactly(luke, leia);
        assertThat(channels).containsExactly(channel + "-a", channel + "-b");
        assertThat(onDuplicatedContext).containsOnly(true);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
        Awaitility.await().until(() -> connectionCount() == before);
        assertThat(connection.sync().pubsubNumpat()).isZero();
    }

    @Test
    void patternOnEndIsCalledAfterTheLastPatternIsUnsubscribed() {
        String first = channel + "-a*";
        String second = channel + "-b*";
        List<Person> people = new CopyOnWriteArrayList<>();
        AtomicInteger ended = new AtomicInteger();

        ReactiveRedisSubscriber subscriber = pubsub
                .subscribeToPatterns(List.of(first, second), (Consumer<Person>) people::add, ended::incrementAndGet, null)
                .await().atMost(TIMEOUT);

        subscriber.unsubscribe(first).await().atMost(TIMEOUT);
        assertThat(ended).hasValue(0);
        pubsub.publish(channel + "-a1", luke).await().atMost(TIMEOUT);
        pubsub.publish(channel + "-b1", leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 1);
        assertThat(people).containsExactly(leia);

        subscriber.unsubscribe(second).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> ended.get() == 1);
    }

    @Test
    void multiSubscribeEmitsMessagesAndCancellationClosesTheConnection() {
        long before = connectionCount();
        List<Person> people = new CopyOnWriteArrayList<>();

        Cancellable cancellable = pubsub.subscribe(channel).subscribe().with(people::add);
        // Wait for the SUBSCRIBE acknowledgement, not only for the connection: a PUBLISH that reaches Redis
        // before the subscription is registered is dropped.
        Awaitility.await().until(() -> connection.sync().pubsubChannels().size() == 1);
        assertThat(connectionCount()).isEqualTo(before + 1);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
        pubsub.publish(channel, leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 2);
        assertThat(people).containsExactly(luke, leia);

        cancellable.cancel();
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void multiSubscribeAsMessagesCarriesTheChannel() {
        String second = channel + "-second";
        List<RedisPubSubMessage<Person>> messages = new CopyOnWriteArrayList<>();

        Cancellable cancellable = pubsub.subscribeAsMessages(channel, second).subscribe().with(messages::add);
        Awaitility.await().until(() -> connection.sync().pubsubChannels().size() == 2);

        pubsub.publish(channel, luke).await().atMost(TIMEOUT);
        pubsub.publish(second, leia).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> messages.size() == 2);
        assertThat(messages).extracting(RedisPubSubMessage::getChannel).containsExactly(channel, second);
        assertThat(messages).extracting(RedisPubSubMessage::getPayload).containsExactly(luke, leia);

        cancellable.cancel();
    }

    @Test
    void multiSubscribeToPatternsEmitsMatchingMessages() {
        long before = connectionCount();
        List<Person> people = new CopyOnWriteArrayList<>();
        List<RedisPubSubMessage<Person>> messages = new CopyOnWriteArrayList<>();

        // Two distinct patterns, both matching the channel published to below: PUBSUB NUMPAT counts unique
        // patterns, so this is how the wait can tell that both subscriptions have been acknowledged.
        Cancellable values = pubsub.subscribeToPatterns(channel + "-*").subscribe().with(people::add);
        Cancellable withChannel = pubsub.subscribeAsMessagesToPatterns(channel + "-?").subscribe().with(messages::add);
        Awaitility.await().until(() -> connection.sync().pubsubNumpat() == 2);
        assertThat(connectionCount()).isEqualTo(before + 2);

        pubsub.publish(channel + "-x", luke).await().atMost(TIMEOUT);
        Awaitility.await().until(() -> people.size() == 1 && messages.size() == 1);
        assertThat(people).containsExactly(luke);
        assertThat(messages.get(0).getChannel()).isEqualTo(channel + "-x");
        assertThat(messages.get(0).getPayload()).isEqualTo(luke);

        values.cancel();
        withChannel.cancel();
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void multiCancelledBeforeTheSubscriptionIsEstablishedStillReleasesTheConnection() {
        long before = connectionCount();
        pubsub.subscribe(channel).subscribe().with(ignored -> {
        }).cancel();
        // The connection may be opened after the cancellation; it must still be closed afterwards.
        Awaitility.await().during(Duration.ofMillis(300)).until(() -> connectionCount() == before);
    }

    @Test
    void unsubscribeAfterTheSubscriptionEndedIsANoOp() {
        long before = connectionCount();
        AtomicInteger ended = new AtomicInteger();
        ReactiveRedisSubscriber subscriber = pubsub.subscribe(List.of(channel), (name, person) -> {
        }, ended::incrementAndGet, null).await().atMost(TIMEOUT);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
        subscriber.unsubscribe().await().atMost(TIMEOUT);
        subscriber.unsubscribe(channel).await().atMost(TIMEOUT);

        Awaitility.await().until(() -> connectionCount() == before);
        Awaitility.await().during(Duration.ofMillis(200)).until(() -> ended.get() == 1);
    }

    @Test
    void concurrentPartialUnsubscribesCloseTheConnectionOnceAndCallOnEndOnce() {
        long before = connectionCount();
        String second = channel + "-second";
        AtomicInteger ended = new AtomicInteger();
        ReactiveRedisSubscriber subscriber = pubsub.subscribe(List.of(channel, second), (name, person) -> {
        }, ended::incrementAndGet, null).await().atMost(TIMEOUT);

        Uni.join().all(subscriber.unsubscribe(channel), subscriber.unsubscribe(second)).andFailFast().await()
                .atMost(TIMEOUT);

        Awaitility.await().until(() -> connectionCount() == before);
        Awaitility.await().during(Duration.ofMillis(200)).until(() -> ended.get() == 1);
    }

    @Test
    void cancellingTheSubscribeUniWhileConnectingClosesTheConnectionOnceItOpens() {
        long before = connectionCount();
        // The connector hands out a connection only when the test says so, which pins the cancellation to the
        // connecting phase: nothing but the cancellation handling can close the connection afterwards.
        CompletableFuture<StatefulRedisPubSubConnection<byte[], byte[]>> gate = new CompletableFuture<>();
        ReactivePubSubCommands<Person> gated = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool(),
                () -> gate).pubsub(Person.class);
        AtomicBoolean completed = new AtomicBoolean();

        Cancellable cancellable = gated.subscribe(channel, person -> {
        }).subscribe().with(subscriber -> completed.set(true), failure -> completed.set(true));
        cancellable.cancel();

        StatefulRedisPubSubConnection<byte[], byte[]> conn = redisClient.connectPubSub(codec, redisUri);
        assertThat(connectionCount()).isEqualTo(before + 1);
        gate.complete(conn);

        Awaitility.await().until(() -> !conn.isOpen());
        Awaitility.await().until(() -> connectionCount() == before);
        assertThat(completed).isFalse();
    }

    @Test
    void retryingTheSubscribeUniAfterATimedOutAttemptSubscribesAfresh() {
        long before = connectionCount();
        // The first connection attempt is held back by the test; every later one goes to Redis. A subscribe that
        // gives up on the first attempt and retries must establish the subscription on the second, and the first
        // attempt's connection must be closed once it is handed out, as the retry's cancellation state is its own.
        CompletableFuture<StatefulRedisPubSubConnection<byte[], byte[]>> gate = new CompletableFuture<>();
        AtomicInteger attempts = new AtomicInteger();
        ReactivePubSubCommands<Person> slowFirst = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool(),
                () -> attempts.incrementAndGet() == 1 ? gate : pubSubConnector().get()).pubsub(Person.class);

        ReactiveRedisSubscriber subscriber = slowFirst.subscribe(channel, person -> {
        }).ifNoItem().after(Duration.ofMillis(200)).fail().onFailure().retry().atMost(1).await().atMost(TIMEOUT);

        assertThat(attempts).hasValue(2);
        assertThat(connection.sync().pubsubChannels()).hasSize(1);

        StatefulRedisPubSubConnection<byte[], byte[]> late = redisClient.connectPubSub(codec, redisUri);
        gate.complete(late);
        Awaitility.await().until(() -> !late.isOpen());

        subscriber.unsubscribe().await().atMost(TIMEOUT);
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void subscribeOnAConnectionClosedMeanwhileFailsAndDoesNotLeak() {
        long before = connectionCount();
        // Lettuce fails SUBSCRIBE on a closed connection; the failure must reach the caller and the connection be
        // let go of.
        StatefulRedisPubSubConnection<byte[], byte[]> dead = redisClient.connectPubSub(codec, redisUri);
        dead.close();
        ReactivePubSubCommands<Person> onDead = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool(),
                () -> CompletableFuture.completedFuture(dead)).pubsub(Person.class);

        assertThatThrownBy(() -> onDead.subscribe(channel, person -> {
        }).await().atMost(TIMEOUT)).hasMessageContaining("closed");

        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void subscribeThrowingSynchronouslyFailsInsteadOfHangingAndClosesTheConnection() {
        long before = connectionCount();
        // Lettuce can refuse a command synchronously (a full command buffer, commands rejected while disconnected).
        // That happens inside the connect callback, where an uncaught throw would be swallowed and the subscribe
        // left pending forever. Simulated by a connection whose async API throws from (P)SUBSCRIBE.
        StatefulRedisPubSubConnection<byte[], byte[]> real = redisClient.connectPubSub(codec, redisUri);
        StatefulRedisPubSubConnection<byte[], byte[]> refusing = refusingSubscribe(real);
        ReactivePubSubCommands<Person> onRefusing = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
                pool(), () -> CompletableFuture.completedFuture(refusing)).pubsub(Person.class);

        assertThatThrownBy(() -> onRefusing.subscribe(channel, person -> {
        }).await().atMost(TIMEOUT)).isInstanceOf(RedisException.class).hasMessage("refused");

        Awaitility.await().until(() -> !real.isOpen());
        Awaitility.await().until(() -> connectionCount() == before);
    }

    /** {@code connection}, except that its async {@code subscribe}/{@code psubscribe} throw instead of dispatching. */
    @SuppressWarnings("unchecked")
    private static StatefulRedisPubSubConnection<byte[], byte[]> refusingSubscribe(
            StatefulRedisPubSubConnection<byte[], byte[]> connection) {
        RedisPubSubAsyncCommands<byte[], byte[]> async = connection.async();
        RedisPubSubAsyncCommands<byte[], byte[]> refusingAsync = (RedisPubSubAsyncCommands<byte[], byte[]>) Proxy
                .newProxyInstance(connection.getClass().getClassLoader(),
                        new Class<?>[] { RedisPubSubAsyncCommands.class }, (proxy, method, args) -> {
                            if (method.getName().equals("subscribe") || method.getName().equals("psubscribe")) {
                                throw new RedisException("refused");
                            }
                            return method.invoke(async, args);
                        });
        return (StatefulRedisPubSubConnection<byte[], byte[]>) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(), new Class<?>[] { StatefulRedisPubSubConnection.class },
                (proxy, method, args) -> method.getName().equals("async") ? refusingAsync
                        : method.invoke(connection, args));
    }

    @Test
    void fullAndPartialUnsubscribesRacingCloseTheConnectionOnceAndCallOnEndOnce() {
        long before = connectionCount();
        String second = channel + "-second";
        AtomicInteger ended = new AtomicInteger();
        ReactiveRedisSubscriber subscriber = pubsub.subscribe(List.of(channel, second), (name, person) -> {
        }, ended::incrementAndGet, null).await().atMost(TIMEOUT);

        // unsubscribe() encodes the subscriber's live list while unsubscribe(channel) shrinks it.
        Uni.join().all(subscriber.unsubscribe(channel), subscriber.unsubscribe()).andFailFast().await().atMost(TIMEOUT);

        Awaitility.await().until(() -> connectionCount() == before);
        Awaitility.await().during(Duration.ofMillis(200)).until(() -> ended.get() == 1);
    }

    @Test
    void droppedConnectionIsReestablishedAndResubscribedWithoutEndingTheSubscriber() {
        long before = connectionCount();
        List<Person> received = new CopyOnWriteArrayList<>();
        AtomicInteger ended = new AtomicInteger();
        ReactiveRedisSubscriber subscriber = pubsub.subscribe(List.of(channel), (name, person) -> received.add(person),
                ended::incrementAndGet, null).await().atMost(TIMEOUT);

        // Kill the subscriber's connection server-side. Lettuce reconnects and re-subscribes (auto-reconnect is on
        // by default): the subscriber keeps receiving and onEnd is not run.
        assertThat(connection.sync().clientKill(KillArgs.Builder.typePubsub())).isEqualTo(1);
        Awaitility.await().until(() -> connection.sync().pubsubChannels().size() == 1);
        pubsub.publish(channel, luke).await().atMost(TIMEOUT);

        Awaitility.await().until(() -> received.equals(List.of(luke)));
        assertThat(ended).hasValue(0);

        subscriber.unsubscribe().await().atMost(TIMEOUT);
        Awaitility.await().until(() -> connectionCount() == before);
        Awaitility.await().until(() -> ended.get() == 1);
    }

    @Test
    void droppedConnectionEndsTheSubscriberWhenAutoReconnectIsOff() {
        long before = connectionCount();
        RedisClient noReconnect = RedisClient.create(lettuceResources.clientResources(), redisUri);
        noReconnect.setOptions(ClientOptions.builder().autoReconnect(false).build());
        try {
            ReactivePubSubCommands<Person> onNoReconnect = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
                    pool(), () -> noReconnect.connectPubSubAsync(codec, redisUri)).pubsub(Person.class);
            AtomicInteger ended = new AtomicInteger();
            AtomicBoolean endedOnDuplicatedContext = new AtomicBoolean();
            ReactiveRedisSubscriber subscriber = onNoReconnect.subscribe(List.of(channel), (name, person) -> {
            }, () -> {
                endedOnDuplicatedContext.set(VertxContext.isOnDuplicatedContext());
                ended.incrementAndGet();
            }, null).await().atMost(TIMEOUT);

            assertThat(connection.sync().clientKill(KillArgs.Builder.typePubsub())).isEqualTo(1);

            Awaitility.await().until(() -> ended.get() == 1);
            assertThat(endedOnDuplicatedContext).isTrue();
            Awaitility.await().until(() -> connectionCount() == before);
            // The subscriber is over: a later unsubscribe is a no-op and onEnd does not run again.
            subscriber.unsubscribe().await().atMost(TIMEOUT);
            Awaitility.await().during(Duration.ofMillis(200)).until(() -> ended.get() == 1);
        } finally {
            noReconnect.shutdown();
        }
    }

    @Test
    void shuttingDownTheClientEndsTheSubscriberAndCompletesTheStream() {
        long before = connectionCount();
        RedisClient shortLived = RedisClient.create(lettuceResources.clientResources(), redisUri);
        ReactivePubSubCommands<Person> onShortLived = new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection,
                pool(), () -> shortLived.connectPubSubAsync(codec, redisUri)).pubsub(Person.class);
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        onShortLived.subscribe(channel).subscribe().with(ignored -> {
        }, failure -> failed.set(true), () -> completed.set(true));
        Awaitility.await().until(() -> connection.sync().pubsubChannels().size() == 1);

        // Closing every connection of the client, as the application does at shutdown, ends the subscription.
        shortLived.shutdown();

        Awaitility.await().until(completed::get);
        assertThat(failed).isFalse();
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void unsubscribeOnAPatternSubscriberValidatesWithThePatternsLabel() {
        ReactiveRedisSubscriber subscriber = pubsub.subscribeToPattern(channel + "-*", person -> {
        }).await().atMost(TIMEOUT);

        assertThatThrownBy(() -> subscriber.unsubscribe(new String[0])).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patterns");

        subscriber.unsubscribe().await().atMost(TIMEOUT);
    }

    @Test
    void invalidPatternArgumentsAreRejectedEagerly() {
        assertThatThrownBy(() -> pubsub.subscribeToPattern((String) null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPattern(channel, (Consumer<Person>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(List.of(" "), (c, p) -> {
        }, null, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(List.of(), p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe(new String[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(channel, null)).isInstanceOf(IllegalArgumentException.class);
    }

}
