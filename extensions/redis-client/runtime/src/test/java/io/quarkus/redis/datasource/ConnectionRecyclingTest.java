package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactivePubSubCommandsImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.redis.client.RedisOptions;

public class ConnectionRecyclingTest extends DatasourceTestBase {

    RedisDataSource ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));

    ReactiveRedisDataSource rds = new ReactiveRedisDataSourceImpl(vertx, redis, api);

    @AfterEach
    public void tearDown() {
        ds.flushall();
    }

    @Test
    void verifyThatConnectionsAreClosed() {
        String k = "increment";
        for (int i = 0; i < 1000; i++) {
            ds.withConnection(x -> x.value(String.class, Integer.class).incr(k));
        }

        assertThat(ds.value(String.class, Integer.class).get(k)).isEqualTo(1000);
    }

    @Test
    void verifyThatConnectionsAreClosedWithTheReactiveDataSource() {
        String k = "increment";
        for (int i = 0; i < 1000; i++) {
            rds.withConnection(x -> x.value(String.class, Integer.class).incr(k)
                    .replaceWithVoid()).await().indefinitely();
        }

        assertThat(rds.value(String.class, Integer.class).get(k).await().indefinitely()).isEqualTo(1000);
    }

    @Test
    void verifyThatConnectionIsClosedWhenAcquisitionIsCancelled() {
        Redis limitedRedis = createLimitedRedis();
        RedisConnection occupiedConnection = limitedRedis.connect().await().indefinitely();
        AtomicBoolean functionInvoked = new AtomicBoolean();

        try {
            ReactiveRedisDataSource limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            Cancellable cancellable = limitedDataSource.withConnection(connection -> {
                functionInvoked.set(true);
                return Uni.createFrom().voidItem();
            }).subscribe().with(ignored -> {
            });

            // cancel before closing the `occupiedConnection`, so the queued acquisition can only complete
            // afterwards and always observes the cancellation
            cancellable.cancel();

            occupiedConnection.closeAndAwait();

            // the connection must be returned to the pool even if it is delivered after cancellation
            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
            assertThat(functionInvoked).isFalse();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatConnectionIsReleasedWhenCancelledWhileFunctionIsRunning() throws InterruptedException {
        Redis limitedRedis = createLimitedRedis();
        CountDownLatch functionStarted = new CountDownLatch(1);

        try {
            ReactiveRedisDataSource limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            Cancellable cancellable = limitedDataSource.withConnection(connection -> {
                functionStarted.countDown();
                // never completes, the connection stays borrowed until the subscription is cancelled
                return Uni.createFrom().nothing();
            }).subscribe().with(ignored -> {
            });

            // make sure the connection was acquired and the function is running before cancelling
            assertThat(functionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            cancellable.cancel();

            // cancelling the running operation must release the borrowed connection back to the pool
            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatConnectionIsClosedWhenTransactionAcquisitionIsCancelled() {
        Redis limitedRedis = createLimitedRedis();
        RedisConnection occupiedConnection = limitedRedis.connect().await().indefinitely();
        AtomicBoolean txInvoked = new AtomicBoolean();

        try {
            ReactiveRedisDataSource limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            Cancellable cancellable = limitedDataSource.withTransaction(tx -> {
                txInvoked.set(true);
                return Uni.createFrom().voidItem();
            }).subscribe().with(ignored -> {
            });

            // cancel before closing the `occupiedConnection`, so the queued acquisition can only complete
            // afterwards and always observes the cancellation
            cancellable.cancel();

            occupiedConnection.closeAndAwait();

            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
            assertThat(txInvoked).isFalse();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatConnectionIsClosedWhenWatchedTransactionAcquisitionIsCancelled() {
        Redis limitedRedis = createLimitedRedis();
        RedisConnection occupiedConnection = limitedRedis.connect().await().indefinitely();
        AtomicBoolean txInvoked = new AtomicBoolean();

        try {
            ReactiveRedisDataSource limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            Cancellable cancellable = limitedDataSource.withTransaction(tx -> {
                txInvoked.set(true);
                return Uni.createFrom().voidItem();
            }, "watched-key").subscribe().with(ignored -> {
            });

            // cancel before closing the `occupiedConnection`, so the queued acquisition can only complete
            // afterwards and always observes the cancellation
            cancellable.cancel();

            occupiedConnection.closeAndAwait();

            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
            assertThat(txInvoked).isFalse();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatConnectionIsClosedWhenOptimisticTransactionAcquisitionIsCancelled() {
        Redis limitedRedis = createLimitedRedis();
        RedisConnection occupiedConnection = limitedRedis.connect().await().indefinitely();
        AtomicBoolean preTxInvoked = new AtomicBoolean();

        try {
            ReactiveRedisDataSource limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            Cancellable cancellable = limitedDataSource.withTransaction(preTx -> {
                preTxInvoked.set(true);
                return Uni.createFrom().item("value");
            }, (input, tx) -> {
                return Uni.createFrom().voidItem();
            }, "watched-key").subscribe().with(ignored -> {
            });

            // cancel before closing the `occupiedConnection`, so the queued acquisition can only complete
            // afterwards and always observes the cancellation
            cancellable.cancel();

            occupiedConnection.closeAndAwait();

            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
            assertThat(preTxInvoked).isFalse();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatConnectionIsClosedWhenSubscribeAcquisitionIsCancelled() {
        Redis limitedRedis = createLimitedRedis();
        RedisConnection occupiedConnection = limitedRedis.connect().await().indefinitely();

        try {
            ReactiveRedisDataSourceImpl limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(limitedDataSource, String.class);
            Cancellable cancellable = pubsub.subscribe("channel", message -> {
            }).subscribe().with(ignored -> {
            });

            // cancel before closing the `occupiedConnection`, so the queued acquisition can only complete
            // afterwards and always observes the cancellation
            cancellable.cancel();

            occupiedConnection.closeAndAwait();

            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void verifyThatSubscriptionConnectionIsKeptOpenAndReleasedOnUnsubscribe() {
        Redis limitedRedis = createLimitedRedis();

        try {
            ReactiveRedisDataSourceImpl limitedDataSource = new ReactiveRedisDataSourceImpl(vertx, limitedRedis,
                    RedisAPI.api(limitedRedis));
            ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(limitedDataSource, String.class);

            ReactivePubSubCommands.ReactiveRedisSubscriber subscriber = pubsub.subscribe("channel", message -> {
            }).await().atMost(Duration.ofSeconds(10));

            subscriber.unsubscribe().await().atMost(Duration.ofSeconds(10));

            RedisConnection recycledConnection = limitedRedis.connect()
                    .await().atMost(Duration.ofSeconds(5));
            recycledConnection.closeAndAwait();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    private Redis createLimitedRedis() {
        return Redis.createClient(vertx, new RedisOptions()
                .setConnectionString(
                        "redis://" + RedisServerExtension.getHost() + ":" + RedisServerExtension.getFirstMappedPort())
                .setMaxPoolSize(1)
                .setMaxPoolWaiting(2));
    }
}
