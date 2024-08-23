package io.quarkus.infinispan.client.runtime.cache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SynchronousInfinispanGet {
    Map<String, Map<Object, CompletableFuture<Object>>> synchronousGetLocks = new ConcurrentHashMap<>();

    public Map<Object, CompletableFuture<Object>> get(String cacheName) {
        return synchronousGetLocks.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());
    }
}
