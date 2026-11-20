package io.quarkus.redis.lettuce.runtime.internal;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * {@link LettuceConnection}s that are connected to nothing, for the tests of the classes that only pass a connection
 * and its command handle around.
 */
final class FakeConnections {

    private FakeConnections() {
    }

    /**
     * A standalone connection whose command handle answers nothing and which only records that it was closed.
     */
    static LettuceConnection standalone() {
        return standalone(new AtomicBoolean());
    }

    /**
     * @param closed set when the connection is closed
     */
    @SuppressWarnings("unchecked")
    static LettuceConnection standalone(AtomicBoolean closed) {
        ClassLoader loader = FakeConnections.class.getClassLoader();
        RedisAsyncCommands<byte[], byte[]> async = (RedisAsyncCommands<byte[], byte[]>) Proxy.newProxyInstance(loader,
                new Class<?>[] { RedisAsyncCommands.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "fake commands";
                    default -> null;
                });
        StatefulRedisConnection<byte[], byte[]> connection = (StatefulRedisConnection<byte[], byte[]>) Proxy
                .newProxyInstance(loader, new Class<?>[] { StatefulRedisConnection.class },
                        (proxy, method, args) -> switch (method.getName()) {
                            case "async" -> async;
                            case "isOpen" -> !closed.get();
                            case "close" -> {
                                closed.set(true);
                                yield null;
                            }
                            case "closeAsync" -> {
                                closed.set(true);
                                yield CompletableFuture.completedFuture(null);
                            }
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            case "toString" -> "fake connection";
                            default -> null;
                        });
        return LettuceConnection.standalone(connection);
    }

}
