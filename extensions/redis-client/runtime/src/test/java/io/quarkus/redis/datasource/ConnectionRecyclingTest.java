package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.api.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.api.RedisDataSource;
import io.quarkus.redis.datasource.impl.BlockingRedisDataSourceImpl;
import io.quarkus.redis.datasource.impl.ReactiveRedisDataSourceImpl;

public class ConnectionRecyclingTest extends DatasourceTestBase {

    RedisDataSource ds = new BlockingRedisDataSourceImpl(redis, api, Duration.ofSeconds(1));

    ReactiveRedisDataSource rds = new ReactiveRedisDataSourceImpl(redis, api);

    @AfterEach
    public void tearDown() {
        ds.flushall();
    }

    @Test
    void verifyThatConnectionsAreClosed() {
        String k = "increment";
        for (int i = 0; i < 1000; i++) {
            ds.withConnection(x -> x.string(String.class, Integer.class).incr(k));
        }

        assertThat(ds.string(String.class, Integer.class).get(k)).isEqualTo(1000);
    }

    @Test
    void verifyThatConnectionsAreClosedWithTheReactiveDataSource() {
        String k = "increment";
        for (int i = 0; i < 1000; i++) {
            rds.withConnection(x -> x.string(String.class, Integer.class).incr(k)
                    .replaceWithVoid()).await().indefinitely();
        }

        assertThat(rds.string(String.class, Integer.class).get(k).await().indefinitely()).isEqualTo(1000);
    }
}
