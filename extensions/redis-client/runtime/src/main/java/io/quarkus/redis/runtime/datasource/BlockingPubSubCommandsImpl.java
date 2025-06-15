package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;

public class BlockingPubSubCommandsImpl<V> extends AbstractRedisCommandGroup implements PubSubCommands<V> {

    private final ReactivePubSubCommands<V> reactive;

    public BlockingPubSubCommandsImpl(RedisDataSource ds, ReactivePubSubCommands<V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void publish(String channel, V message) {
        reactive.publish(channel, message).await().atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribe(String channel, Consumer<V> onMessage) {
        return subscribe(channel, onMessage, null, null);
    }

    @Override
    public RedisSubscriber subscribeToPattern(String pattern, Consumer<V> onMessage) {
        return subscribeToPattern(pattern, onMessage, null, null);
    }

    @Override
    public RedisSubscriber subscribeToPatterns(List<String> patterns, Consumer<V> onMessage) {
        return subscribeToPatterns(patterns, onMessage, null, null);
    }

    @Override
    public RedisSubscriber subscribe(List<String> channels, Consumer<V> onMessage) {
        return subscribe(channels, onMessage, null, null);
    }

    @Override
    public RedisSubscriber subscribe(String channel, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(channel, "channel");
        return subscribe(List.of(channel), onMessage, onEnd, onException);
    }

    @Override
    public RedisSubscriber subscribeToPattern(String pattern, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(pattern, "pattern");
        return subscribeToPatterns(List.of(pattern), onMessage, onEnd, onException);
    }

    @Override
    public RedisSubscriber subscribeToPatterns(List<String> patterns, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return reactive.subscribeToPatterns(patterns, onMessage, onEnd, onException)
                .map(rs -> new BlockingRedisSubscriber(rs)).await().atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribe(List<String> channels, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return reactive.subscribe(channels, onMessage, onEnd, onException).map(r -> new BlockingRedisSubscriber(r))
                .await().atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribeToPattern(String pattern, BiConsumer<String, V> onMessage) {
        return reactive.subscribeToPattern(pattern, onMessage).map(r -> new BlockingRedisSubscriber(r)).await()
                .atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage) {
        return reactive.subscribeToPatterns(patterns, onMessage).map(r -> new BlockingRedisSubscriber(r)).await()
                .atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribeToPattern(String pattern, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return reactive.subscribeToPattern(pattern, onMessage, onEnd, onException)
                .map(r -> new BlockingRedisSubscriber(r)).await().atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return reactive.subscribeToPatterns(patterns, onMessage, onEnd, onException)
                .map(r -> new BlockingRedisSubscriber(r)).await().atMost(timeout);
    }

    @Override
    public RedisSubscriber subscribe(List<String> channels, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return reactive.subscribe(channels, onMessage, onEnd, onException).map(r -> new BlockingRedisSubscriber(r))
                .await().atMost(timeout);
    }

    private class BlockingRedisSubscriber implements RedisSubscriber {
        private final ReactivePubSubCommands.ReactiveRedisSubscriber reactiveRedisSubscriber;

        public BlockingRedisSubscriber(ReactivePubSubCommands.ReactiveRedisSubscriber reactiveRedisSubscriber) {
            this.reactiveRedisSubscriber = reactiveRedisSubscriber;
        }

        @Override
        public void unsubscribe(String... channels) {
            reactiveRedisSubscriber.unsubscribe(channels).await().atMost(timeout);
        }

        @Override
        public void unsubscribe() {
            reactiveRedisSubscriber.unsubscribe().await().atMost(timeout);
        }
    }
}
