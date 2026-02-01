package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

public class FailOnAbortedSetTest extends DatasourceTestBase {

    @Test
    void setWithNxEx_DefaultDoesNotFailOnAbort() {
        ReactiveRedisDataSource ds = new ReactiveRedisDataSourceImpl(vertx, redis, api, false);
        ReactiveValueCommands<String, String> values = ds.value(String.class);

        String key = "reactive-set-abort-default-" + UUID.randomUUID();
        ds.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        values.set(key, "v1", new SetArgs().nx().ex(10)).await().atMost(Duration.ofSeconds(5));
        values.set(key, "v2", new SetArgs().nx().ex(10)).await().atMost(Duration.ofSeconds(5));

        assertThat(values.get(key).await().atMost(Duration.ofSeconds(5))).isEqualTo("v1");
    }

    @Test
    void setWithNxEx_FlagEnabledFailsOnAbort() {
        ReactiveRedisDataSource ds = new ReactiveRedisDataSourceImpl(vertx, redis, api, true);
        ReactiveValueCommands<String, String> values = ds.value(String.class);

        String key = "reactive-set-abort-enabled-" + UUID.randomUUID();
        ds.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        values.set(key, "v1", new SetArgs().nx().ex(10)).await().atMost(Duration.ofSeconds(5));

        assertThatThrownBy(() -> values.set(key, "v2", new SetArgs().nx().ex(10)).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(RedisCommandAbortedException.class);

        assertThat(values.get(key).await().atMost(Duration.ofSeconds(5))).isEqualTo("v1");
    }

    @Test
    void setWithXxEx_FlagEnabledFailsOnAbort() {
        ReactiveRedisDataSource ds = new ReactiveRedisDataSourceImpl(vertx, redis, api, true);
        ReactiveValueCommands<String, String> values = ds.value(String.class);

        String key = "reactive-set-abort-xx-enabled-" + UUID.randomUUID();
        ds.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        assertThatThrownBy(() -> values.set(key, "v1", new SetArgs().xx().ex(10)).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(RedisCommandAbortedException.class);

        assertThat(values.get(key).await().atMost(Duration.ofSeconds(5))).isNull();
    }

    @Test
    void setWithNxEx_UsingStringSetArgs_FlagEnabledFailsOnAbort() {
        ReactiveRedisDataSource ds = new ReactiveRedisDataSourceImpl(vertx, redis, api, true);
        io.quarkus.redis.datasource.string.ReactiveStringCommands<String, String> strings = ds.string(String.class);

        String key = "reactive-set-abort-stringargs-enabled-" + UUID.randomUUID();
        ds.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        strings.set(key, "v1", new io.quarkus.redis.datasource.string.SetArgs().nx().ex(10)).await()
                .atMost(Duration.ofSeconds(5));

        assertThatThrownBy(() -> strings.set(key, "v2", new io.quarkus.redis.datasource.string.SetArgs().nx().ex(10)).await()
                .atMost(Duration.ofSeconds(5)))
                .isInstanceOf(RedisCommandAbortedException.class);

        assertThat(ds.value(String.class).get(key).await().atMost(Duration.ofSeconds(5))).isEqualTo("v1");
    }

    @Test
    void setWithNxGet_DoesNotFailOnFlagEnabled() {
        ReactiveRedisDataSource ds = new ReactiveRedisDataSourceImpl(vertx, redis, api, true);
        io.quarkus.redis.datasource.string.ReactiveStringCommands<String, String> strings = ds.string(String.class);

        String key = "reactive-set-abort-nx-get-enabled-" + UUID.randomUUID();
        ds.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        strings.set(key, "v1", new io.quarkus.redis.datasource.string.SetArgs().nx().get()).await()
                .atMost(Duration.ofSeconds(5));

        assertThat(ds.value(String.class).get(key).await().atMost(Duration.ofSeconds(5))).isEqualTo("v1");
    }

    @Test
    void blockingSetWithNxEx_FlagEnabledFailsOnAbort() {
        ReactiveRedisDataSourceImpl reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api, true);
        BlockingRedisDataSourceImpl ds = new BlockingRedisDataSourceImpl(reactive, Duration.ofSeconds(5));

        String key = "blocking-set-abort-enabled-" + UUID.randomUUID();
        reactive.key(String.class).del(key).await().atMost(Duration.ofSeconds(5));

        ds.value(String.class).set(key, "v1", new SetArgs().nx().ex(10));

        assertThatThrownBy(() -> ds.value(String.class).set(key, "v2", new SetArgs().nx().ex(10)))
                .isInstanceOf(RedisCommandAbortedException.class);

        assertThat(ds.value(String.class).get(key)).isEqualTo("v1");
    }
}
