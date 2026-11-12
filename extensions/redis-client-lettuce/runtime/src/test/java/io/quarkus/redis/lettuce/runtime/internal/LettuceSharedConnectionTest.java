package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.Cancellable;

class LettuceSharedConnectionTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /** A connector whose attempts the test completes, one future per attempt. */
    private static final class Connector implements Supplier<CompletionStage<LettuceConnection>> {

        final List<CompletableFuture<LettuceConnection>> attempts = new CopyOnWriteArrayList<>();

        @Override
        public CompletionStage<LettuceConnection> get() {
            CompletableFuture<LettuceConnection> attempt = new CompletableFuture<>();
            attempts.add(attempt);
            return attempt;
        }
    }

    @Test
    void nothingConnectsUntilTheFirstUse() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);

        shared.connect();

        assertThat(connector.attempts).isEmpty();
        assertThat(shared.established()).isNull();
        assertThat(shared.isCluster()).isFalse();
        assertThat(new LettuceSharedConnection(connector, true).isCluster()).isTrue();
    }

    @Test
    void concurrentFirstUsesOpenOneConnection() throws Exception {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);
        int users = 16;
        ExecutorService executor = Executors.newFixedThreadPool(users);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<UniAssertSubscriber<LettuceConnection>> subscribers = new CopyOnWriteArrayList<>();
            CountDownLatch subscribed = new CountDownLatch(users);
            for (int i = 0; i < users; i++) {
                executor.submit(() -> {
                    start.await();
                    subscribers.add(shared.connect().subscribe().withSubscriber(UniAssertSubscriber.create()));
                    subscribed.countDown();
                    return null;
                });
            }
            start.countDown();
            assertThat(subscribed.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();

            assertThat(connector.attempts).hasSize(1);
            LettuceConnection connection = FakeConnections.standalone();
            connector.attempts.get(0).complete(connection);

            for (UniAssertSubscriber<LettuceConnection> subscriber : subscribers) {
                assertThat(subscriber.awaitItem(TIMEOUT).getItem()).isSameAs(connection);
            }
            assertThat(shared.established()).isSameAs(connection);
            // an established connection is handed out at once, and nothing connects again
            assertThat(shared.connect().await().atMost(TIMEOUT)).isSameAs(connection);
            assertThat(connector.attempts).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void waitersAreServedInArrivalOrderBeforeTheConnectionIsPublished() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);
        List<String> served = new ArrayList<>();
        List<Boolean> publishedWhileServing = new ArrayList<>();

        shared.connect().subscribe().with(connection -> {
            served.add("first");
            publishedWhileServing.add(shared.established() != null);
            // arrives while the waiters are being served: queues behind them instead of overtaking them
            shared.connect().subscribe().with(late -> {
                served.add("late");
                publishedWhileServing.add(shared.established() != null);
            });
        });
        shared.connect().subscribe().with(connection -> served.add("second"));
        shared.connect().subscribe().with(connection -> served.add("third"));

        LettuceConnection connection = FakeConnections.standalone();
        connector.attempts.get(0).complete(connection);

        assertThat(served).containsExactly("first", "second", "third", "late");
        assertThat(publishedWhileServing).containsExactly(false, false);
        assertThat(shared.established()).isSameAs(connection);
    }

    @Test
    void aFailedAttemptFailsItsWaitersAndTheNextUseTriesAgain() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);
        UniAssertSubscriber<LettuceConnection> first = shared.connect().subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<LettuceConnection> second = shared.connect().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        RuntimeException failure = new IllegalStateException("nobody listens");
        connector.attempts.get(0).completeExceptionally(failure);

        assertThat(first.awaitFailure(TIMEOUT).getFailure()).isSameAs(failure);
        assertThat(second.awaitFailure(TIMEOUT).getFailure()).isSameAs(failure);
        assertThat(shared.established()).isNull();

        UniAssertSubscriber<LettuceConnection> third = shared.connect().subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        assertThat(connector.attempts).hasSize(2);
        LettuceConnection connection = FakeConnections.standalone();
        connector.attempts.get(1).complete(connection);

        assertThat(third.awaitItem(TIMEOUT).getItem()).isSameAs(connection);
        assertThat(shared.established()).isSameAs(connection);
    }

    @Test
    void aConnectorThatThrowsFailsTheUseAndTheNextOneTriesAgain() {
        RuntimeException failure = new IllegalArgumentException("bad address");
        AtomicInteger attempts = new AtomicInteger();
        LettuceConnection connection = FakeConnections.standalone();
        LettuceSharedConnection shared = new LettuceSharedConnection(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw failure;
            }
            return CompletableFuture.completedFuture(connection);
        }, false);

        assertThatThrownBy(() -> shared.connect().await().atMost(TIMEOUT)).isSameAs(failure);
        assertThat(shared.connect().await().atMost(TIMEOUT)).isSameAs(connection);
        assertThat(attempts).hasValue(2);
    }

    @Test
    void aCancelledWaiterDoesNotKeepTheOthersWaiting() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);
        AtomicBoolean cancelledServed = new AtomicBoolean();
        Cancellable cancelled = shared.connect().subscribe().with(connection -> cancelledServed.set(true));
        UniAssertSubscriber<LettuceConnection> other = shared.connect().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        cancelled.cancel();
        LettuceConnection connection = FakeConnections.standalone();
        connector.attempts.get(0).complete(connection);

        assertThat(other.awaitItem(TIMEOUT).getItem()).isSameAs(connection);
        assertThat(cancelledServed).isFalse();
        assertThat(shared.established()).isSameAs(connection);
    }

    @Test
    void closeClosesTheEstablishedConnectionAndFailsLaterUses() {
        AtomicBoolean closed = new AtomicBoolean();
        LettuceConnection connection = FakeConnections.standalone(closed);
        AtomicInteger attempts = new AtomicInteger();
        LettuceSharedConnection shared = new LettuceSharedConnection(() -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(connection);
        }, false);
        shared.connect().await().atMost(TIMEOUT);

        shared.close();
        shared.close();

        assertThat(closed).isTrue();
        assertThat(shared.established()).isNull();
        assertThatThrownBy(() -> shared.connect().await().atMost(TIMEOUT)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
        assertThat(attempts).hasValue(1);
    }

    @Test
    void closeBeforeAnyUseOpensNothing() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);

        shared.close();

        assertThatThrownBy(() -> shared.connect().await().atMost(TIMEOUT)).isInstanceOf(IllegalStateException.class);
        assertThat(connector.attempts).isEmpty();
    }

    @Test
    void closeWhileConnectingClosesTheConnectionOnceItIsEstablished() {
        Connector connector = new Connector();
        LettuceSharedConnection shared = new LettuceSharedConnection(connector, false);
        UniAssertSubscriber<LettuceConnection> waiter = shared.connect().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        shared.close();
        AtomicBoolean closed = new AtomicBoolean();
        connector.attempts.get(0).complete(FakeConnections.standalone(closed));

        assertThat(waiter.awaitFailure(TIMEOUT).getFailure()).isInstanceOf(IllegalStateException.class);
        assertThat(closed).isTrue();
        assertThat(shared.established()).isNull();
    }

}
