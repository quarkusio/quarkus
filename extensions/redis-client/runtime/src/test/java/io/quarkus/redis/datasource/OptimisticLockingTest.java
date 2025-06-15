package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.mutiny.Uni;

public class OptimisticLockingTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void hashPutIfPresent() {
        OptimisticLockingTransactionResult<Boolean> result = blocking.withTransaction(ds -> {
            HashCommands<String, String, String> hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                tx.hash(String.class).hset(key, "field", "Bar");
            } else {
                tx.discard();
            }
        }, key);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isFalse();

        blocking.hash(String.class).hset(key, "field", "Foo");

        result = blocking.withTransaction(ds -> {
            HashCommands<String, String, String> hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                tx.hash(String.class).hset(key, "field", "Bar");
            } else {
                tx.discard();
            }
        }, key);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.discarded()).isFalse();
        assertThat(result.getPreTransactionResult()).isTrue();

        assertThat(blocking.hash(String.class).hget(key, "field")).isEqualTo("Bar");

    }

    @Test
    public void hashPutIfPresentReactive() {
        OptimisticLockingTransactionResult<Boolean> result = reactive.withTransaction(ds -> {
            var hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely();

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isFalse();

        blocking.hash(String.class).hset(key, "field", "Foo");

        result = reactive.withTransaction(ds -> {
            var hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely();

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.discarded()).isFalse();
        assertThat(result.getPreTransactionResult()).isTrue();

        assertThat(blocking.hash(String.class).hget(key, "field")).isEqualTo("Bar");

    }

    @Test
    public void hashPutIfPresentWithModificationOfTheWatchKey() {
        OptimisticLockingTransactionResult<Boolean> result = blocking.withTransaction(ds -> {

            // Using another connection - update the key
            blocking.hash(String.class).hset(key, "another", "hello");

            HashCommands<String, String, String> hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                tx.hash(String.class).hset(key, "field", "Bar");
            } else {
                tx.discard();
            }
        }, key);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isFalse();

        blocking.hash(String.class).hset(key, "field", "Foo");

        result = blocking.withTransaction(ds -> {
            // Using another connection - update the key
            blocking.hash(String.class).hset(key, "yet-another", "hello");

            HashCommands<String, String, String> hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                tx.hash(String.class).hset(key, "field", "Bar");
            } else {
                tx.discard();
            }
        }, key);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isTrue();

        assertThat(blocking.hash(String.class).hget(key, "field")).isEqualTo("Foo");

    }

    @Test
    public void hashPutIfPresentReactiveWithModificationOfTheWatchKey() {
        OptimisticLockingTransactionResult<Boolean> result = reactive.withTransaction(ds -> {
            // Using another connection - update the key
            return reactive.hash(String.class).hset(key, "another", "hello").chain(() -> {
                var hashCommands = ds.hash(String.class);
                return hashCommands.hexists(key, "field");
            });
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely();

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isFalse();

        blocking.hash(String.class).hset(key, "field", "Foo");

        result = reactive.withTransaction(ds -> {
            // Using another connection - update the key
            return reactive.hash(String.class).hset(key, "another", "hello").chain(() -> {
                var hashCommands = ds.hash(String.class);
                return hashCommands.hexists(key, "field");
            });
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely();

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.discarded()).isTrue();
        assertThat(result.getPreTransactionResult()).isTrue();

        assertThat(blocking.hash(String.class).hget(key, "field")).isEqualTo("Foo");

    }

    @Test
    public void hashPutIfPresentPreBlockFailing() {
        assertThatThrownBy(() -> blocking.withTransaction(ds -> {
            Assertions.fail("expected");
            HashCommands<String, String, String> hashCommands = ds.hash(String.class);
            return hashCommands.hexists(key, "field");
        }, (i, tx) -> {
            if (i) {
                tx.hash(String.class).hset(key, "field", "Bar");
            } else {
                tx.discard();
            }
        }, key)).hasMessageContaining("expected");
    }

    @Test
    public void hashPutIfPresentPreBlockProducingAFailure() {
        assertThatThrownBy(() -> reactive.<Boolean> withTransaction(ds -> {
            return Uni.createFrom().failure(new RuntimeException("expected"));
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely()).hasMessageContaining("expected");
    }

    @Test
    public void hashPutIfPresentPreBlockThrowingAnException() {
        assertThatThrownBy(() -> reactive.withTransaction(ds -> {
            Assertions.fail("expected");
            return Uni.createFrom().item(true);
        }, (i, tx) -> {
            if (i) {
                return tx.hash(String.class).hset(key, "field", "Bar").replaceWithVoid();
            } else {
                return tx.discard();
            }
        }, key).await().indefinitely()).hasMessageContaining("expected");
    }

    @Test
    public void testZpop() {
        var list = blocking.sortedSet(String.class);
        list.zadd(key, 1.0, "a");
        list.zadd(key, 2.0, "b");
        list.zadd(key, 3.0, "c");

        var res = blocking.withTransaction(ds -> {
            var elements = ds.sortedSet(String.class).zrange(key, 0, 0);
            if (!elements.isEmpty()) {
                return elements.get(0);
            }
            return null;
        }, (element, tx) -> {
            if (element == null) {
                tx.discard();
            } else {
                tx.sortedSet(String.class).zrem(key, element);
            }
        }, key);

        assertThat(res.discarded()).isFalse();
        assertThat(res.getPreTransactionResult()).isEqualTo("a");
    }

    @Test
    public void testZpopReactive() {
        var list = blocking.sortedSet(String.class);
        list.zadd(key, 1.0, "a");
        list.zadd(key, 2.0, "b");
        list.zadd(key, 3.0, "c");

        var res = reactive.withTransaction(ds -> {
            return ds.sortedSet(String.class).zrange(key, 0, 0).map(elements -> {
                if (elements.isEmpty()) {
                    return null;
                }
                return elements.get(0);
            });
        }, (element, tx) -> {
            if (element == null) {
                return tx.discard();
            } else {
                return tx.sortedSet(String.class).zrem(key, element);
            }
        }, key).await().indefinitely();

        assertThat(res.discarded()).isFalse();
        assertThat(res.getPreTransactionResult()).isEqualTo("a");
    }

    @Test
    public void testZpopWithKeyModification() {
        var list = blocking.sortedSet(String.class);
        list.zadd(key, 1.0, "a");
        list.zadd(key, 2.0, "b");
        list.zadd(key, 3.0, "c");

        var res = blocking.withTransaction(ds -> {
            blocking.sortedSet(String.class).zadd(key, 4.0, "d");
            var elements = ds.sortedSet(String.class).zrange(key, 0, 0);
            if (!elements.isEmpty()) {
                return elements.get(0);
            }
            return null;
        }, (element, tx) -> {
            if (element == null) {
                tx.discard();
            } else {
                tx.sortedSet(String.class).zrem(key, element);
            }
        }, key);

        assertThat(res.discarded()).isTrue();
        assertThat(res.getPreTransactionResult()).isEqualTo("a");
    }

}
