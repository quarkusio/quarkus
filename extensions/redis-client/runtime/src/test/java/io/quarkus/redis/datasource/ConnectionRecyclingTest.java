package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

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
            rds.withConnection(x -> x.value(String.class, Integer.class).incr(k).replaceWithVoid()).await()
                    .indefinitely();
        }

        assertThat(rds.value(String.class, Integer.class).get(k).await().indefinitely()).isEqualTo(1000);
    }
}
