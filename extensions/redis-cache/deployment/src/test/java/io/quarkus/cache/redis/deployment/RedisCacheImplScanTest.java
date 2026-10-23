package io.quarkus.cache.redis.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.redis.runtime.RedisCacheImpl;
import io.quarkus.cache.redis.runtime.RedisCacheInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import io.vertx.redis.client.impl.RequestImpl;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;

/**
 * Plain unit tests (no Redis server) for the {@code SCAN} iteration of {@link RedisCacheImpl#invalidateIf}.
 */
public class RedisCacheImplScanTest {

    private static final String PREFIX = "cache:test:";

    @Test
    public void testScanDoesNotOverflowTheStackWhenRepliesAreSynchronous() throws InterruptedException {
        int pages = 100_000;
        FakeRedis redis = new FakeRedis(pages, false);
        RedisCacheImpl cache = new RedisCacheImpl(cacheInfo(), null, redis, () -> false);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                cache.invalidateAll().await().atMost(Duration.ofSeconds(30));
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "scan", 1024 * 1024);
        thread.start();
        thread.join(Duration.ofSeconds(60));

        assertThat(thread.isAlive()).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(redis.scans).isEqualTo(pages);
        assertThat(redis.deletedKeys).hasSize(pages);
    }

    @Test
    public void testScanFailsWhenAPageCannotBeProcessed() {
        FakeRedis redis = new FakeRedis(3, true);
        RedisCacheImpl cache = new RedisCacheImpl(cacheInfo(), null, redis, () -> false);

        assertThatThrownBy(() -> cache.invalidateAll().await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    public void testScanRestartsFromTheBeginningOnResubscription() {
        FakeRedis redis = new FakeRedis(5, false);
        RedisCacheImpl cache = new RedisCacheImpl(cacheInfo(), null, redis, () -> false);
        Uni<Void> invalidation = cache.invalidateAll();

        invalidation.await().atMost(Duration.ofSeconds(5));
        invalidation.await().atMost(Duration.ofSeconds(5));

        assertThat(redis.cursors).containsExactly("0", "1", "2", "3", "4", "0", "1", "2", "3", "4");
        assertThat(redis.deletedKeys).hasSize(10);
    }

    private static RedisCacheInfo cacheInfo() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "test";
        return info;
    }

    /**
     * Replies synchronously to {@code SCAN} with one key per page, and records {@code DEL} arguments.
     */
    static class FakeRedis extends Redis {

        final int pages;
        final boolean failOnLastPage;
        final List<String> cursors = new ArrayList<>();
        final List<String> deletedKeys = new ArrayList<>();
        int scans;

        FakeRedis(int pages, boolean failOnLastPage) {
            super((Object) null);
            this.pages = pages;
            this.failOnLastPage = failOnLastPage;
        }

        @Override
        public Uni<Response> send(Request command) {
            List<byte[]> args = ((RequestImpl) command).getArgs();
            if (command.command() == Command.SCAN) {
                scans++;
                String cursor = new String(args.get(0), StandardCharsets.UTF_8);
                cursors.add(cursor);
                int page = Integer.parseInt(cursor);
                String next = page + 1 == pages ? "0" : String.valueOf(page + 1);
                MultiType reply = MultiType.create(2, false);
                reply.add(bulk(next));
                if (failOnLastPage && page + 1 == pages) {
                    reply.add(new FailingResponse());
                } else {
                    MultiType keys = MultiType.create(1, false);
                    keys.add(bulk(PREFIX + "k" + page));
                    reply.add(keys);
                }
                return Uni.createFrom().item(reply);
            }
            if (command.command() == Command.DEL) {
                for (byte[] arg : args) {
                    deletedKeys.add(new String(arg, StandardCharsets.UTF_8));
                }
                return Uni.createFrom().item(bulk("OK"));
            }
            throw new UnsupportedOperationException(command.command().toString());
        }

        private static Response bulk(String value) {
            return BulkType.create(Buffer.buffer(value), false);
        }
    }

    static class FailingResponse implements Response {

        @Override
        public ResponseType type() {
            return ResponseType.MULTI;
        }

        @Override
        public Iterator<Response> iterator() {
            throw new IllegalStateException("boom");
        }

        @Override
        public String toString() {
            return "failing";
        }
    }
}
