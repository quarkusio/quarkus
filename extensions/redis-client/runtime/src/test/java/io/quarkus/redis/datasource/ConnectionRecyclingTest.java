package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
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
    void verifyThatConnectionsAreClosedWhenAcquisitionIsCancelled() {
        Redis limitedRedis = Redis.createClient(vertx, new RedisOptions()
                .setConnectionString(
                        "redis://" + RedisServerExtension.getHost() + ":" + RedisServerExtension.getFirstMappedPort())
                .setMaxPoolSize(1)
                .setMaxPoolWaiting(2));
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

            cancellable.cancel();
            occupiedConnection.closeAndAwait();

            RedisConnection recycledConnection = limitedRedis.connect()
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            recycledConnection.closeAndAwait();
            assertThat(functionInvoked).isFalse();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }
}
