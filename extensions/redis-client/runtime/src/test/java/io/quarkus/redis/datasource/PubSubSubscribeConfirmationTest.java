package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands.ReactiveRedisSubscriber;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactivePubSubCommandsImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.VerticleBase;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

/**
 * Verifies that a subscription completes whatever the order of the SUBSCRIBE write completion and the server
 * confirmation, and that it fails and releases its connection when no confirmation arrives.
 */
public class PubSubSubscribeConfirmationTest extends DatasourceTestBase {

    @Test
    void subscribeFromAnEventLoopContextCompletes() throws Exception {
        CompletableFuture<Context> started = new CompletableFuture<>();
        String deployment = vertx.getDelegate().deployVerticle(new VerticleBase() {
            @Override
            public Future<?> start() {
                started.complete(context);
                return Future.succeededFuture();
            }
        }).toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

        try {
            ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(
                    new ReactiveRedisDataSourceImpl(vertx, redis, api), String.class);
            CompletableFuture<ReactiveRedisSubscriber> subscribed = new CompletableFuture<>();
            // a duplicated context of a verticle's event-loop context, as an HTTP request has
            VertxContext.createNewDuplicatedContext(started.get(5, TimeUnit.SECONDS)).runOnContext(
                    ignored -> pubsub.subscribe("race-" + key, message -> {
                    }).subscribe().with(subscribed::complete, subscribed::completeExceptionally));

            ReactiveRedisSubscriber subscriber = subscribed.get(5, TimeUnit.SECONDS);
            subscriber.unsubscribe().await().atMost(Duration.ofSeconds(5));
        } finally {
            vertx.getDelegate().undeploy(deployment).toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void subscribeCompletesWhenTheConfirmationIsReadBeforeTheWriteCompletes() {
        // the SUBSCRIBE send completes long after the server has confirmed the subscription
        Redis delayedRedis = wrap(redis, conn -> new DelegatingRedisConnection(conn) {
            @Override
            public Future<Response> send(Request command) {
                Future<Response> sent = super.send(command);
                if (!command.command().equals(Command.SUBSCRIBE)) {
                    return sent;
                }
                Promise<Response> delayed = Promise.promise();
                sent.onComplete(ar -> vertx.getDelegate().setTimer(500, ignored -> delayed.handle(ar)));
                return delayed.future();
            }
        });
        ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(
                new ReactiveRedisDataSourceImpl(vertx, delayedRedis, RedisAPI.api(delayedRedis)), String.class);

        ReactiveRedisSubscriber subscriber = pubsub.subscribe("delayed-" + key, message -> {
        }).await().atMost(Duration.ofSeconds(5));
        subscriber.unsubscribe().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void zeroSubscribeTimeoutDisablesTheTimeout() {
        ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(
                new ReactiveRedisDataSourceImpl(vertx, redis, api, Duration.ZERO), String.class);

        ReactiveRedisSubscriber subscriber = pubsub.subscribe("no-timeout-" + key, message -> {
        }).await().atMost(Duration.ofSeconds(5));
        subscriber.unsubscribe().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void subscribeFailsAndReleasesTheConnectionWhenNoConfirmationArrives() {
        Redis limitedRedis = Redis.createClient(vertx, new RedisOptions()
                .setConnectionString(
                        "redis://" + RedisServerExtension.getHost() + ":" + RedisServerExtension.getFirstMappedPort())
                .setMaxPoolSize(1)
                .setMaxPoolWaiting(2));
        try {
            // the reply handler is never installed, so the confirmation is lost
            Redis deafRedis = wrap(limitedRedis, conn -> new DelegatingRedisConnection(conn) {
                @Override
                public io.vertx.redis.client.RedisConnection handler(Handler<Response> handler) {
                    return this;
                }
            });
            ReactivePubSubCommandsImpl<String> pubsub = new ReactivePubSubCommandsImpl<>(
                    new ReactiveRedisDataSourceImpl(vertx, deafRedis, RedisAPI.api(deafRedis), Duration.ofMillis(500)),
                    String.class);

            assertThatThrownBy(() -> pubsub.subscribe("deaf-" + key, message -> {
            }).await().atMost(Duration.ofSeconds(5)))
                    .hasMessageContaining("No subscription confirmation received from Redis");

            // the failed subscription must give its connection back to the pool
            RedisConnection recycled = limitedRedis.connect().await().atMost(Duration.ofSeconds(5));
            assertThat(recycled).isNotNull();
            recycled.closeAndAwait();
        } finally {
            limitedRedis.closeAndAwait();
        }
    }

    @Test
    void subscribeTimeoutAppliesToSingleConnectionDataSources() {
        // the reply handler is never installed, so the confirmation is lost
        Redis deafRedis = wrap(redis, conn -> new DelegatingRedisConnection(conn) {
            @Override
            public io.vertx.redis.client.RedisConnection handler(Handler<Response> handler) {
                return this;
            }
        });
        ReactiveRedisDataSourceImpl ds = new ReactiveRedisDataSourceImpl(vertx, deafRedis, RedisAPI.api(deafRedis),
                Duration.ofMillis(500));

        assertThatThrownBy(() -> ds.withConnection(conn -> conn.pubsub(String.class).subscribe("deaf-" + key, message -> {
        }).replaceWithVoid()).await().atMost(Duration.ofSeconds(5)))
                .hasMessageContaining("No subscription confirmation received from Redis");

        BlockingRedisDataSourceImpl blocking = new BlockingRedisDataSourceImpl(ds, Duration.ofSeconds(5));
        assertThatThrownBy(() -> blocking.withConnection(conn -> conn.pubsub(String.class).subscribe("deaf-" + key,
                message -> {
                })))
                .hasMessageContaining("No subscription confirmation received from Redis");
    }

    private static Redis wrap(Redis redis,
            Function<io.vertx.redis.client.RedisConnection, io.vertx.redis.client.RedisConnection> wrapper) {
        return new Redis(redis.getDelegate()) {
            @Override
            public Uni<RedisConnection> connect() {
                return super.connect().map(conn -> new RedisConnection(wrapper.apply(conn.getDelegate())));
            }
        };
    }
}
