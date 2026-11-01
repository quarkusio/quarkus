package io.quarkus.redis.lettuce.runtime.internal.pubsub;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.pubsub.RedisPubSubMessage;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceResult;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.DefaultRedisPubSubMessage;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;

public class LettuceReactivePubSubCommandsImpl<V> extends AbstractLettuceCommands<V, V>
        implements ReactivePubSubCommands<V> {

    private final LettuceReactiveRedisDataSourceImpl dataSource;
    private final Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector;

    public LettuceReactivePubSubCommandsImpl(LettuceReactiveRedisDataSourceImpl dataSource, LettuceConnection connection,
            Supplier<CompletionStage<StatefulRedisPubSubConnection<byte[], byte[]>>> pubSubConnector,
            Type classOfMessage) {
        super(connection, classOfMessage, classOfMessage, new Marshaller(classOfMessage));
        this.dataSource = dataSource;
        this.pubSubConnector = pubSubConnector;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> publish(String channel, V message) {
        nonNull(channel, "channel");
        nonNull(message, "message");
        return LettuceCommand.discarding(() -> async.publish(marshaller.encode(channel), marshaller.encode(message)))
                .toUni();
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(String channel, Consumer<V> onMessage) {
        return subscribe(channel, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage) {
        return subscribeToPattern(pattern, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, BiConsumer<String, V> onMessage) {
        return subscribeToPattern(pattern, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage) {
        return subscribeToPatterns(patterns, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage) {
        return subscribeToPatterns(patterns, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage) {
        return subscribe(channels, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, BiConsumer<String, V> onMessage) {
        return subscribe(channels, onMessage, null, null);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(String channel, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(channel, "channel");
        return subscribe(List.of(channel), onMessage, onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(pattern, "pattern");
        return subscribeToPatterns(List.of(pattern), onMessage, onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(pattern, "pattern");
        return subscribeToPatterns(List.of(pattern), onMessage, onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(onMessage, "onMessage");
        return subscribeToPatterns(patterns, (ignored, value) -> onMessage.accept(value), onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage,
            Runnable onEnd, Consumer<Throwable> onException) {
        validateCollection(patterns, "patterns");
        nonNull(onMessage, "onMessage");

        List<String> subscribed = List.copyOf(patterns);
        return open(conn -> new ReactiveLettucePatternSubscriber(conn, subscribed, onMessage, onEnd, onException));
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(onMessage, "onMessage");
        return subscribe(channels, (ignored, value) -> onMessage.accept(value), onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        validateCollection(channels, "channels");
        nonNull(onMessage, "onMessage");

        List<String> subscribed = List.copyOf(channels);
        return open(conn -> new ReactiveLettuceChannelSubscriber(conn, subscribed, onMessage, onEnd, onException));
    }

    private static void validateCollection(Collection<String> col, String name) {
        notNullOrEmpty(col, name);

        for (String element : col) {
            if (element == null) {
                throw new IllegalArgumentException("Element must not be `null`");
            }
            if (element.isBlank()) {
                throw new IllegalArgumentException("Element must not be `blank`");
            }
        }
    }

    private Uni<ReactiveRedisSubscriber> open(
            Function<StatefulRedisPubSubConnection<byte[], byte[]>, AbstractReactiveLettuceSubscriber> factory) {
        return Uni.createFrom().deferred(() -> establish(factory));
    }

    private Uni<ReactiveRedisSubscriber> establish(
            Function<StatefulRedisPubSubConnection<byte[], byte[]>, AbstractReactiveLettuceSubscriber> factory) {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<AbstractReactiveLettuceSubscriber> opened = new AtomicReference<>();
        Uni<ReactiveRedisSubscriber> establishing = LettuceResult.toUni(() -> {
            CompletableFuture<ReactiveRedisSubscriber> result = new CompletableFuture<>();
            pubSubConnector.get().whenComplete((conn, connectFailure) -> {
                if (connectFailure != null) {
                    result.completeExceptionally(connectFailure);
                    return;
                }
                // A throw out of a whenComplete callback goes nowhere: the result must be failed, and the
                // connection closed, by hand.
                AbstractReactiveLettuceSubscriber subscriber;
                try {
                    subscriber = factory.apply(conn);
                } catch (RuntimeException e) {
                    conn.closeAsync();
                    result.completeExceptionally(e);
                    return;
                }
                opened.set(subscriber);
                if (cancelled.get()) {
                    subscriber.discard();
                    return;
                }
                try {
                    // Lettuce can refuse a command synchronously, e.g. on a connection closed in the meantime.
                    subscriber.subscribe().whenComplete((ignored, subscribeFailure) -> {
                        if (subscribeFailure != null) {
                            subscriber.discard();
                            result.completeExceptionally(subscribeFailure);
                        } else if (!result.complete(subscriber) || cancelled.get()) {
                            subscriber.discard();
                        }
                    });
                } catch (RuntimeException e) {
                    subscriber.discard();
                    result.completeExceptionally(e);
                }
            });
            return result;
        });
        return establishing.onCancellation().invoke(() -> {
            cancelled.set(true);
            AbstractReactiveLettuceSubscriber subscriber = opened.get();
            if (subscriber != null) {
                subscriber.discard();
            }
        });
    }

    @Override
    public Multi<V> subscribe(String... channels) {
        notNullOrEmpty(channels, "channels");
        doesNotContainNull(channels, "channels");
        List<String> list = List.of(channels);
        return stream(emitter -> subscribe(list, (channel, value) -> emitter.emit(value), emitter::complete, emitter::fail));
    }

    @Override
    public Multi<RedisPubSubMessage<V>> subscribeAsMessages(String... channels) {
        notNullOrEmpty(channels, "channels");
        doesNotContainNull(channels, "channels");
        List<String> list = List.of(channels);
        return stream(emitter -> subscribe(list,
                (channel, value) -> emitter.emit(new DefaultRedisPubSubMessage<>(value, channel)),
                emitter::complete, emitter::fail));
    }

    @Override
    public Multi<V> subscribeToPatterns(String... patterns) {
        notNullOrEmpty(patterns, "patterns");
        doesNotContainNull(patterns, "patterns");
        List<String> list = List.of(patterns);
        return stream(emitter -> subscribeToPatterns(list, (channel, value) -> emitter.emit(value), emitter::complete,
                emitter::fail));
    }

    @Override
    public Multi<RedisPubSubMessage<V>> subscribeAsMessagesToPatterns(String... patterns) {
        notNullOrEmpty(patterns, "patterns");
        doesNotContainNull(patterns, "patterns");
        List<String> list = List.of(patterns);
        return stream(emitter -> subscribeToPatterns(list,
                (channel, value) -> emitter.emit(new DefaultRedisPubSubMessage<>(value, channel)),
                emitter::complete, emitter::fail));
    }

    private <T> Multi<T> stream(Function<MultiEmitter<? super T>, Uni<ReactiveRedisSubscriber>> subscription) {
        return Multi.createFrom().emitter(emitter -> subscription.apply(emitter)
                .subscribe().with(
                        subscriber -> emitter.onTermination(() -> subscriber.unsubscribe().subscribe().with(
                                ignored -> {
                                })),
                        emitter::fail));
    }

    private abstract class AbstractReactiveLettuceSubscriber implements ReactiveRedisSubscriber {

        final StatefulRedisPubSubConnection<byte[], byte[]> connection;
        final List<String> names;
        final BiConsumer<String, V> onMessage;
        final Runnable onEnd;
        final Consumer<Throwable> onException;
        final Vertx vertx = dataSource.getVertx();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final RedisPubSubListener<byte[], byte[]> listener = new RedisPubSubAdapter<>() {
            @Override
            public void message(byte[] channel, byte[] message) {
                deliver(channel, message);
            }

            @Override
            public void message(byte[] pattern, byte[] channel, byte[] message) {
                deliver(channel, message);
            }
        };

        private final RedisConnectionStateListener stateListener = new RedisConnectionStateListener() {
            @Override
            public void onRedisDisconnected(RedisChannelHandler<?, ?> handler) {
                if (handler.isClosed() || !connection.getOptions().isAutoReconnect()) {
                    end();
                }
            }

            @Override
            public void onRedisExceptionCaught(RedisChannelHandler<?, ?> handler, Throwable cause) {
                if (onException != null) {
                    runOnDuplicatedContext(() -> onException.accept(cause));
                }
            }
        };

        AbstractReactiveLettuceSubscriber(StatefulRedisPubSubConnection<byte[], byte[]> connection, List<String> names,
                BiConsumer<String, V> onMessage, Runnable onEnd, Consumer<Throwable> onException) {
            this.connection = connection;
            this.names = new CopyOnWriteArrayList<>(names);
            this.onMessage = onMessage;
            this.onEnd = onEnd;
            this.onException = onException;
        }

        private byte[][] encode(List<String> names) {
            return names.stream().map(marshaller::encode).toArray(byte[][]::new);
        }

        private void runOnDuplicatedContext(Runnable runnable) {
            Context context = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
            VertxContextSafetyToggle.setContextSafe(context, true);
            context.runOnContext(ignored -> runnable.run());
        }

        abstract String kind();

        abstract RedisFuture<Void> subscribeToRedis(byte[][] names);

        abstract RedisFuture<Void> unsubscribeFromRedis(byte[][] names);

        CompletionStage<Void> subscribe() {
            connection.addListener(listener);
            connection.addListener(stateListener);
            return subscribeToRedis(encode(names));
        }

        void discard() {
            if (closed.compareAndSet(false, true)) {
                removeListeners();
                connection.closeAsync();
            }
        }

        private void end() {
            if (closed.compareAndSet(false, true)) {
                removeListeners();
                names.clear();
                connection.closeAsync();
                if (onEnd != null) {
                    runOnDuplicatedContext(onEnd);
                }
            }
        }

        private void removeListeners() {
            connection.removeListener(listener);
            connection.removeListener(stateListener);
        }

        void deliver(byte[] channel, byte[] payload) {
            runOnDuplicatedContext(() -> {
                V value;
                try {
                    value = decodeValue(payload);
                } catch (RuntimeException e) {
                    if (onException == null) {
                        throw e;
                    }
                    onException.accept(e);
                    return;
                }
                onMessage.accept(decodeString(channel), value);
            });
        }

        @Override
        public Uni<Void> unsubscribe(String... names) {
            notNullOrEmpty(names, kind());
            doesNotContainNull(names, kind());
            List<String> list = List.of(names);
            return unsubscribe(list, () -> this.names.removeAll(list));
        }

        @Override
        public Uni<Void> unsubscribe() {
            return unsubscribe(names, names::clear);
        }

        private Uni<Void> unsubscribe(List<String> list, Runnable forget) {
            if (closed.get()) {
                return Uni.createFrom().voidItem();
            }
            return LettuceResult.toUni(() -> unsubscribeFromRedis(encode(list)))
                    // A concurrent unsubscribe may have closed the connection under this one. That is the
                    // outcome the caller asked for, so it is not reported as a failure.
                    .onFailure().recoverWithUni(failure -> closed.get() ? Uni.createFrom().voidItem()
                            : Uni.createFrom().failure(failure))
                    .chain(() -> {
                        forget.run();
                        return closeIfNothingLeft();
                    });
        }

        private Uni<Void> closeIfNothingLeft() {
            if (!names.isEmpty() || !closed.compareAndSet(false, true)) {
                return Uni.createFrom().voidItem();
            }
            removeListeners();
            return LettuceResult.toUni(connection::closeAsync).replaceWithVoid().invoke(() -> {
                if (onEnd != null) {
                    runOnDuplicatedContext(onEnd);
                }
            });
        }

    }

    private class ReactiveLettuceChannelSubscriber extends AbstractReactiveLettuceSubscriber {

        ReactiveLettuceChannelSubscriber(StatefulRedisPubSubConnection<byte[], byte[]> connection, List<String> channels,
                BiConsumer<String, V> onMessage, Runnable onEnd, Consumer<Throwable> onException) {
            super(connection, channels, onMessage, onEnd, onException);
        }

        @Override
        String kind() {
            return "channels";
        }

        @Override
        RedisFuture<Void> subscribeToRedis(byte[][] channels) {
            return connection.async().subscribe(channels);
        }

        @Override
        RedisFuture<Void> unsubscribeFromRedis(byte[][] channels) {
            return connection.async().unsubscribe(channels);
        }

    }

    private class ReactiveLettucePatternSubscriber extends AbstractReactiveLettuceSubscriber {

        ReactiveLettucePatternSubscriber(StatefulRedisPubSubConnection<byte[], byte[]> connection, List<String> patterns,
                BiConsumer<String, V> onMessage, Runnable onEnd, Consumer<Throwable> onException) {
            super(connection, patterns, onMessage, onEnd, onException);
        }

        @Override
        String kind() {
            return "patterns";
        }

        @Override
        RedisFuture<Void> subscribeToRedis(byte[][] patterns) {
            return connection.async().psubscribe(patterns);
        }

        @Override
        RedisFuture<Void> unsubscribeFromRedis(byte[][] patterns) {
            return connection.async().punsubscribe(patterns);
        }

    }

}
