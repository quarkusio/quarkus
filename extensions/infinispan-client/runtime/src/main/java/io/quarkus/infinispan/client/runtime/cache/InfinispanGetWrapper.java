package io.quarkus.infinispan.client.runtime.cache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.CacheException;

import io.smallrye.mutiny.Uni;

public class InfinispanGetWrapper {
    final RemoteCache cache;
    final Map<Object, CompletableFuture<Object>> synchronousGets;

    public InfinispanGetWrapper(RemoteCache cache, Map<Object, CompletableFuture<Object>> synchronousGetLocks) {
        this.cache = cache;
        this.synchronousGets = synchronousGetLocks;
    }

    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        return Uni.createFrom().completionStage(
                /*
                 * Even if CompletionStage is eager, the Supplier used below guarantees that the cache value computation
                 * will be delayed until subscription time. In other words, the cache value computation is done lazily.
                 */
                new Supplier<CompletionStage<V>>() {
                    @Override
                    public CompletionStage<V> get() {
                        CompletionStage<Object> infinispanValue = getFromInfinispan(key, valueLoader);
                        return cast(infinispanValue);
                    }
                });
    }

    private <K, V> CompletableFuture<Object> getFromInfinispan(K key, Function<K, V> valueLoader) {
        CompletableFuture<Object> stage = new CompletableFuture<>();
        CompletableFuture<Object> prev;
        try {
            if ((prev = synchronousGets.putIfAbsent(key, stage)) != null) {
                return prev;
            }
            cache.getAsync(key).whenComplete((valGet, tGet) -> {
                if (tGet != null) {
                    stage.completeExceptionally((Throwable) tGet);
                    synchronousGets.remove(key);
                } else if (valGet != null) {
                    stage.complete(valGet);
                    synchronousGets.remove(key);
                } else {
                    Object newValue = valueLoader.apply(key);
                    if (newValue == null) {
                        synchronousGets.remove(key);
                        stage.complete(null);
                    } else {
                        cache.putIfAbsentAsync(key, newValue).whenComplete((valPut, tPut) -> {
                            if (tPut != null) {
                                stage.completeExceptionally((Throwable) tPut);
                            } else {
                                stage.complete(valPut == null ? newValue : valPut);
                            }
                            synchronousGets.remove(key);
                        });
                    }
                }
            });

        } catch (Exception ex) {
            stage.completeExceptionally(ex);
            synchronousGets.remove(key);
        }
        return stage;
    }

    private <T> T cast(Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new CacheException(
                    "An existing cached value type does not match the type returned by the value loading function", e);
        }
    }
}
