package io.quarkus.redis.datasource.impl;

import static io.quarkus.redis.datasource.impl.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.api.pubsub.ReactivePubSubCommands;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.RedisConnection;

public class ReactivePubSubCommandsImpl<V> extends AbstractRedisCommands implements ReactivePubSubCommands<V> {

    private final Class<V> classOfMessage;
    private final Redis ds;

    public ReactivePubSubCommandsImpl(ReactiveRedisDataSourceImpl ds, Class<V> classOfMessage) {
        super(ds, new Marshaller(classOfMessage));
        this.ds = ds.redis;
        this.classOfMessage = classOfMessage;
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
        return subscribe(List.of(channel), onMessage);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage) {
        return subscribeToPatterns(List.of(pattern), onMessage);
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage) {
        notNullOrEmpty(patterns, "patterns");
        nonNull(onMessage, "onMessage");

        for (String pattern : patterns) {
            if (pattern == null) {
                throw new IllegalArgumentException("Patterns must not be null");
            }
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("Patterns cannot be blank");
            }
        }

        return ds.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveRedisPatternSubscriberImpl subscriber = new ReactiveRedisPatternSubscriberImpl(conn, api, onMessage,
                            patterns);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    @Override
    public Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage) {
        notNullOrEmpty(channels, "channels");
        nonNull(onMessage, "onMessage");

        for (String channel : channels) {
            if (channel == null) {
                throw new IllegalArgumentException("Channels must not be null");
            }
            if (channel.isBlank()) {
                throw new IllegalArgumentException("Channels cannot be blank");
            }
        }

        return ds.connect()
                .chain(conn -> {
                    RedisAPI api = RedisAPI.api(conn);
                    ReactiveAbstractRedisSubscriberImpl subscriber = new ReactiveAbstractRedisSubscriberImpl(conn, api,
                            onMessage, channels);
                    return subscriber.subscribe()
                            .replaceWith(subscriber);
                });
    }

    @Override
    public Multi<V> subscribe(String... channels) {
        notNullOrEmpty(channels, "channels");
        doesNotContainNull(channels, "channels");

        return Multi.createFrom().emitter(emitter -> {
            subscribe(List.of(channels), emitter::emit)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> x.unsubscribe(channels).subscribe().asCompletionStage());
                    }, emitter::fail);
        });
    }

    @Override
    public Multi<V> subscribeToPatterns(String... patterns) {
        notNullOrEmpty(patterns, "patterns");
        doesNotContainNull(patterns, "patterns");

        return Multi.createFrom().emitter(emitter -> {
            subscribeToPatterns(List.of(patterns), emitter::emit)
                    .subscribe().with(x -> {
                        emitter.onTermination(() -> x.unsubscribe(patterns).subscribe().asCompletionStage());
                    }, emitter::fail);
        });
    }

    private abstract class AbstractRedisSubscriber implements ReactiveRedisSubscriber {
        final RedisConnection connection;
        final RedisAPI api;
        final String id;
        final Consumer<V> onMessage;

        private AbstractRedisSubscriber(RedisConnection connection, RedisAPI api, Consumer<V> onMessage) {
            this.connection = connection;
            this.api = api;
            this.id = UUID.randomUUID().toString();
            this.onMessage = onMessage;
        }

        abstract Uni<Void> subscribeToRedis();

        public Uni<String> subscribe() {
            Uni<Void> handled = Uni.createFrom().emitter(emitter -> {
                connection.handler(r -> {
                    if (r != null && r.size() > 0) {
                        Context context = VertxContext.getOrCreateDuplicatedContext(Vertx.currentContext());
                        String command = r.get(0).toString();
                        if ("subscribe".equalsIgnoreCase(command) || "psubscribe".equalsIgnoreCase(command)) {
                            emitter.complete(null); // Subscribed
                        } else if ("message".equalsIgnoreCase(command)) {
                            context.runOnContext(x -> onMessage.accept(marshaller.decode(classOfMessage, r.get(2))));
                        } else if ("pmessage".equalsIgnoreCase(command)) {
                            context.runOnContext(x -> onMessage.accept(marshaller.decode(classOfMessage, r.get(3))));
                        }
                    }
                });
            });

            Uni<Void> subscribed = subscribeToRedis();

            return subscribed.chain(() -> handled)
                    .replaceWith(id);
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

        public ReactiveAbstractRedisSubscriberImpl(RedisConnection connection, RedisAPI api, Consumer<V> onMessage,
                List<String> channels) {
            super(connection, api, onMessage);
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

        public ReactiveRedisPatternSubscriberImpl(RedisConnection connection, RedisAPI api, Consumer<V> onMessage,
                List<String> patterns) {
            super(connection, api, onMessage);
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
