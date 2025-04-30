package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.pubsub.RedisPubSubMessage;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;
import io.vertx.mutiny.redis.client.Response;

public class ReactivePubSubCommandsImpl<V> extends AbstractRedisCommands implements ReactivePubSubCommands<V> {

    private final Type classOfMessage;
    private final Redis client;
    private final ReactiveRedisDataSourceImpl datasource;

    public ReactivePubSubCommandsImpl(ReactiveRedisDataSourceImpl ds, Type classOfMessage) {
        super(ds, new Marshaller(classOfMessage));
        this.client = ds.redis;
        this.datasource = ds;
        this.classOfMessage = classOfMessage;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return datasource;
    }

    @Override
    public Uni<Void> publish(String channel, V message) {
        nonNull(channel, "channel");
        nonNull(message, "message");
        RedisCommand cmd = RedisCommand.of(Command.PUBLISH)
                .put(channel)
                .put(marshaller.encode(message));
        return execute(cmd)
                .replaceWithVoid();
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
        return subscribe(List.of(channel), onMessage, onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return subscribeToPatterns(List.of(pattern), onMessage, onEnd, onException);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        return subscribeToPatterns(List.of(pattern), onMessage, onEnd, onException);
    }

    private void validatePatterns(List<String> patterns) {
        notNullOrEmpty(patterns, "patterns");

        for (String pattern : patterns) {
            if (pattern == null) {
                throw new IllegalArgumentException("Pattern must not be null");
            }
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("Pattern cannot be blank");
            }
        }
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(onMessage, "onMessage");
        validatePatterns(patterns);

        return client.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveRedisPatternSubscriberImpl subscriber = new ReactiveRedisPatternSubscriberImpl(conn, api, patterns,
                            (channel, value) -> onMessage.accept(value), onEnd, onException);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage,
            Runnable onEnd,
            Consumer<Throwable> onException) {
        validatePatterns(patterns);
        nonNull(onMessage, "onMessage");

        return client.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveRedisPatternSubscriberImpl subscriber = new ReactiveRedisPatternSubscriberImpl(conn, api, patterns,
                            onMessage, onEnd, onException);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    private void validateChannels(List<String> channels) {
        notNullOrEmpty(channels, "channels");

        for (String pattern : channels) {
            if (pattern == null) {
                throw new IllegalArgumentException("Channel must not be null");
            }
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("Channel cannot be blank");
            }
        }
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        nonNull(onMessage, "onMessage");
        validateChannels(channels);

        return client.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveAbstractRedisSubscriberImpl subscriber = new ReactiveAbstractRedisSubscriberImpl(conn, api,
                            channels, (channel, value) -> onMessage.accept(value), onEnd, onException);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException) {
        notNullOrEmpty(channels, "channels");
        nonNull(onMessage, "onMessage");

        for (String channel : channels) {
            if (channel == null) {
                return Uni.createFrom().failure(new IllegalArgumentException("Channels must not be null"));
            }
            if (channel.isBlank()) {
                return Uni.createFrom().failure(new IllegalArgumentException("Channels cannot be blank"));
            }
        }

        return client.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveAbstractRedisSubscriberImpl subscriber = new ReactiveAbstractRedisSubscriberImpl(conn, api,
                            channels, onMessage, onEnd, onException);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    @Override
    public Multi<V> subscribeToPatterns(String... patterns) {
        notNullOrEmpty(patterns, "patterns");
        doesNotContainNull(patterns, "patterns");

        return Multi.createFrom().emitter(emitter -> {
            subscribeToPatterns(List.of(patterns), emitter::emit, emitter::complete, emitter::fail)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> x.unsubscribe(patterns).subscribe().asCompletionStage());
                    }, emitter::fail);
        });
    }

    @Override
    public Multi<RedisPubSubMessage<V>> subscribeAsMessagesToPatterns(String... patterns) {
        notNullOrEmpty(patterns, "patterns");
        doesNotContainNull(patterns, "patterns");
        return Multi.createFrom().emitter(emitter -> {
            subscribeToPatterns(List.of(patterns),
                    (channel, value) -> emitter.emit(new DefaultRedisPubSubMessage<>(value, channel)), emitter::complete,
                    emitter::fail)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> x.unsubscribe(patterns).subscribe().asCompletionStage());
                    }, emitter::fail);
        });
    }

    @Override
    public Multi<V> subscribe(String... channels) {
        notNullOrEmpty(channels, "channels");
        doesNotContainNull(channels, "channels");

        return Multi.createFrom().emitter(emitter -> {
            subscribe(List.of(channels), emitter::emit, emitter::complete, emitter::fail)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> {
                            x.unsubscribe(channels).subscribe().asCompletionStage();
                        });
                    }, emitter::fail);
        });
    }

    @Override
    public Multi<RedisPubSubMessage<V>> subscribeAsMessages(String... channels) {
        notNullOrEmpty(channels, "channels");
        doesNotContainNull(channels, "channels");

        List<String> list = List.of(channels);
        return Multi.createFrom().emitter(emitter -> {
            subscribe(list,
                    (channel, value) -> emitter.emit(new DefaultRedisPubSubMessage<>(value, channel)),
                    emitter::complete, emitter::fail)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> {
                            x.unsubscribe(channels).subscribe().asCompletionStage();
                        });
                    }, emitter::fail);
        });
    }

    private abstract class AbstractRedisSubscriber implements ReactiveRedisSubscriber {
        final RedisConnection connection;
        final RedisAPI api;
        final String id;
        final BiConsumer<String, V> onMessage;
        final Runnable onEnd;
        final Consumer<Throwable> onException;

        private AbstractRedisSubscriber(RedisConnection connection, RedisAPI api, BiConsumer<String, V> onMessage,
                Runnable onEnd, Consumer<Throwable> onException) {
            this.connection = connection;
            this.api = api;
            this.id = UUID.randomUUID().toString();
            this.onMessage = onMessage;
            this.onEnd = onEnd;
            this.onException = onException;
        }

        abstract Uni<Void> subscribeToRedis();

        public Uni<String> subscribe() {
            Uni<Void> handled = Uni.createFrom().emitter(emitter -> {
                connection.handler(r -> runOnDuplicatedContext(() -> handleRedisEvent(emitter, r)));
                if (onEnd != null) {
                    connection.endHandler(() -> runOnDuplicatedContext(onEnd));
                }
                if (onException != null) {
                    connection.exceptionHandler(t -> runOnDuplicatedContext(() -> onException.accept(t)));
                }
            });

            Uni<Void> subscribed = subscribeToRedis();

            return subscribed.chain(() -> handled)
                    .replaceWith(id);
        }

        private void runOnDuplicatedContext(Runnable runnable) {
            Consumer<Context> contextConsumer = c -> {
                Context context = VertxContext.getOrCreateDuplicatedContext(c);
                VertxContextSafetyToggle.setContextSafe(context, true);
                context.runOnContext(ignored -> runnable.run());
            };
            Optional.ofNullable(Vertx.currentContext()).ifPresentOrElse(contextConsumer,
                    () -> datasource.getVertx().runOnContext(() -> contextConsumer.accept(Vertx.currentContext())));
        }

        protected void handleRedisEvent(UniEmitter<? super Void> emitter, Response r) {
            if (r != null && r.size() > 0) {
                String command = r.get(0).toString();
                if ("subscribe".equalsIgnoreCase(command) || "psubscribe".equalsIgnoreCase(command)) {
                    emitter.complete(null); // Subscribed
                } else if ("message".equalsIgnoreCase(command)) {
                    onMessage.accept(r.get(1).toString(), marshaller.decode(classOfMessage, r.get(2)));
                } else if ("pmessage".equalsIgnoreCase(command)) {
                    onMessage.accept(r.get(2).toString(), marshaller.decode(classOfMessage, r.get(3)));
                }
            }
        }

        public Uni<Void> closeAndUnregister(Collection<?> collection) {
            if (collection.isEmpty()) {
                return connection.close();
            }
            return Uni.createFrom().voidItem();
        }

    }

    private class ReactiveAbstractRedisSubscriberImpl extends AbstractRedisSubscriber implements ReactiveRedisSubscriber {

        private final List<String> channels;

        public ReactiveAbstractRedisSubscriberImpl(RedisConnection connection, RedisAPI api, List<String> channels,
                BiConsumer<String, V> onMessage, Runnable onEnd,
                Consumer<Throwable> onException) {
            super(connection, api, onMessage, onEnd, onException);
            this.channels = new ArrayList<>(channels);
        }

        @Override
        Uni<Void> subscribeToRedis() {
            return api.subscribe(channels).replaceWithVoid();
        }

        @Override
        public Uni<Void> unsubscribe(String... channels) {
            notNullOrEmpty(channels, "channels");
            doesNotContainNull(channels, "channels");
            List<String> list = List.of(channels);
            return api.unsubscribe(list)
                    .chain(() -> {
                        this.channels.removeAll(list);
                        return closeAndUnregister(this.channels);
                    });
        }

        @Override
        public Uni<Void> unsubscribe() {
            return api.unsubscribe(channels)
                    .chain(() -> {
                        this.channels.clear();
                        return closeAndUnregister(channels);
                    });
        }
    }

    private class ReactiveRedisPatternSubscriberImpl extends AbstractRedisSubscriber implements ReactiveRedisSubscriber {

        private final List<String> patterns;

        public ReactiveRedisPatternSubscriberImpl(RedisConnection connection, RedisAPI api, List<String> patterns,
                BiConsumer<String, V> onMessage, Runnable onEnd,
                Consumer<Throwable> onException) {
            super(connection, api, onMessage, onEnd, onException);
            this.patterns = new ArrayList<>(patterns);
        }

        @Override
        Uni<Void> subscribeToRedis() {
            return api.psubscribe(patterns).replaceWithVoid();
        }

        @Override
        public Uni<Void> unsubscribe(String... patterns) {
            notNullOrEmpty(patterns, "patterns");
            doesNotContainNull(patterns, "patterns");
            List<String> list = List.of(patterns);
            return api.punsubscribe(list)
                    .chain(() -> {
                        this.patterns.removeAll(list);
                        return closeAndUnregister(this.patterns);
                    });
        }

        @Override
        public Uni<Void> unsubscribe() {
            return api.punsubscribe(patterns)
                    .chain(() -> {
                        this.patterns.clear();
                        return closeAndUnregister(patterns);
                    });
        }
    }

}
