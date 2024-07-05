package io.quarkus.cache.infinispan.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.protocol.Codec27;
import org.infinispan.commons.util.NullValue;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.reactivestreams.FlowAdapters;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.cache.Cache;
import io.quarkus.cache.runtime.AbstractCache;
import io.quarkus.infinispan.client.runtime.InfinispanClientProducer;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * This class is an internal Quarkus cache implementation using Infinispan.
 * Do not use it explicitly from your Quarkus application.
 */
public class InfinispanCacheImpl extends AbstractCache implements Cache {

    private final RemoteCache remoteCache;
    private final InfinispanCacheInfo cacheInfo;
    private final Map<Object, CompletableFuture> computationResults = new ConcurrentHashMap<>();
    private final long lifespan;
    private final long maxIdle;

    public InfinispanCacheImpl(InfinispanCacheInfo cacheInfo, RemoteCache remoteCache) {
        this.cacheInfo = cacheInfo;
        this.remoteCache = remoteCache;
        this.lifespan = cacheInfo.lifespan.map(l -> l.toMillis()).orElse(-1L);
        this.maxIdle = cacheInfo.maxIdle.map(m -> m.toMillis()).orElse(-1L);
    }

    public InfinispanCacheImpl(InfinispanCacheInfo cacheInfo,
            Optional<String> infinispanClientName) {
        this(cacheInfo, determineInfinispanClient(infinispanClientName, cacheInfo.name));
    }

    private static RemoteCache determineInfinispanClient(Optional<String> infinispanCacheName, String cacheName) {
        ArcContainer container = Arc.container();
        InfinispanClientProducer producer = container.instance(InfinispanClientProducer.class).get();
        return producer.getRemoteCache(infinispanCacheName.orElse(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME),
                cacheName);
    }

    @Override
    public String getName() {
        return Objects.requireNonNullElse(cacheInfo.name, "default-infinispan-cache");
    }

    @Override
    public Object getDefaultKey() {
        return "default-key";
    }

    private Object encodeNull(Object value) {
        return value != null ? value : NullValue.NULL;
    }

    private <T> T decodeNull(Object value) {
        return value != NullValue.NULL ? (T) value : null;
    }

    @Override
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        return Uni.createFrom()
                .completionStage(() -> CompletionStages.handleAndCompose(remoteCache.getAsync(key), (v1, ex1) -> {
                    if (ex1 != null) {
                        return CompletableFuture.failedFuture(ex1);
                    }

                    if (v1 != null) {
                        return CompletableFuture.completedFuture(decodeNull(v1));
                    }

                    CompletableFuture<V> resultAsync = new CompletableFuture<>();
                    CompletableFuture<V> computedValue = computationResults.putIfAbsent(key, resultAsync);
                    if (computedValue != null) {
                        return computedValue;
                    }
                    V newValue = valueLoader.apply(key);
                    remoteCache
                            .putIfAbsentAsync(key, encodeNull(newValue), lifespan, TimeUnit.MILLISECONDS, maxIdle,
                                    TimeUnit.MILLISECONDS)
                            .whenComplete((existing, ex2) -> {
                                if (ex2 != null) {
                                    resultAsync.completeExceptionally((Throwable) ex2);
                                } else if (existing == null) {
                                    resultAsync.complete(newValue);
                                } else {
                                    resultAsync.complete(decodeNull(existing));
                                }
                                computationResults.remove(key);
                            });
                    return resultAsync;
                }));
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader) {
        Context context = Vertx.currentContext();

        return Uni.createFrom().completionStage(CompletionStages.handleAndCompose(remoteCache.getAsync(key), (v1, ex1) -> {
            if (ex1 != null) {
                return CompletableFuture.failedFuture(ex1);
            }

            if (v1 != null) {
                return CompletableFuture.completedFuture(decodeNull(v1));
            }

            CompletableFuture<V> resultAsync = new CompletableFuture<>();
            CompletableFuture<V> computedValue = computationResults.putIfAbsent(key, resultAsync);
            if (computedValue != null) {
                return computedValue;
            }
            valueLoader.apply(key).convert().toCompletionStage()
                    .whenComplete((newValue, ex2) -> {
                        if (ex2 != null) {
                            resultAsync.completeExceptionally(ex2);
                            computationResults.remove(key);
                        } else {
                            remoteCache.putIfAbsentAsync(key, encodeNull(newValue), lifespan, TimeUnit.MILLISECONDS, maxIdle,
                                    TimeUnit.MILLISECONDS).whenComplete((existing, ex3) -> {
                                        if (ex3 != null) {
                                            resultAsync.completeExceptionally((Throwable) ex3);
                                        } else if (existing == null) {
                                            resultAsync.complete(newValue);
                                        } else {
                                            resultAsync.complete(decodeNull(existing));
                                        }
                                        computationResults.remove(key);
                                    });
                        }
                    });
            return resultAsync;
        })).emitOn(new Executor() {
            // We need make sure we go back to the original context when the cache value is computed.
            // Otherwise, we would always emit on the context having computed the value, which could
            // break the duplicated context isolation.
            @Override
            public void execute(Runnable command) {
                Context ctx = Vertx.currentContext();
                if (context == null) {
                    // We didn't capture a context
                    if (ctx == null) {
                        // We are not on a context => we can execute immediately.
                        command.run();
                    } else {
                        // We are on a context.
                        // We cannot continue on the current context as we may share a duplicated context.
                        // We need a new one. Note that duplicate() does not duplicate the duplicated context,
                        // but the root context.
                        ((ContextInternal) ctx).duplicate()
                                .runOnContext(new Handler<Void>() {
                                    @Override
                                    public void handle(Void ignored) {
                                        command.run();
                                    }
                                });
                    }
                } else {
                    // We captured a context.
                    if (ctx == context) {
                        // We are on the same context => we can execute immediately
                        command.run();
                    } else {
                        // 1) We are not on a context (ctx == null) => we need to switch to the captured context.
                        // 2) We are on a different context (ctx != null) => we need to switch to the captured context.
                        context.runOnContext(new Handler<Void>() {
                            @Override
                            public void handle(Void ignored) {
                                command.run();
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        return Uni.createFrom().completionStage(() -> remoteCache.removeAsync(key));
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().completionStage(() -> remoteCache.clearAsync());
    }

    @Override
    public Uni<Void> invalidateIf(Predicate<Object> predicate) {
        Flow.Publisher<Map.Entry> entriesPublisher = FlowAdapters
                .toFlowPublisher(remoteCache.publishEntries(Codec27.EMPTY_VALUE_CONVERTER, null, null, 512));
        return Uni.createFrom().multi(Multi.createFrom().publisher(entriesPublisher)
                .map(e -> ((Map.Entry<Object, Object>) e).getKey())
                .filter(key -> predicate.test(key))
                .onItem()
                .call(key -> Uni.createFrom().completionStage(remoteCache.removeAsync(key))))
                .replaceWithVoid();
    }

    @Override
    public <T extends Cache> T as(Class<T> type) {
        if (type.getTypeName().equals(InfinispanCacheImpl.class.getTypeName())) {
            return (T) this;
        }

        throw new IllegalArgumentException("Class type not supported : " + type);
    }
}
