package io.quarkus.cache.redis.deployment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
public class ComplexCachedService {
    static final String CACHE_NAME_GENERIC = "test-cache-generic";
    static final String CACHE_NAME_ARRAY = "test-cache-array";
    static final String CACHE_NAME_GENERIC_ARRAY = "test-cache-generic-array";

    @CacheResult(cacheName = CACHE_NAME_GENERIC)
    public List<String> genericReturnType(String key) {
        return List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    @CacheResult(cacheName = CACHE_NAME_ARRAY)
    public int[] arrayReturnType(String key) {
        int[] result = new int[2];
        result[0] = ThreadLocalRandom.current().nextInt();
        result[1] = ThreadLocalRandom.current().nextInt();
        return result;
    }

    @CacheResult(cacheName = CACHE_NAME_GENERIC_ARRAY)
    public List<? extends CharSequence>[] genericArrayReturnType(String key) {
        return new List[] {
                List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
                List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        };
    }
}
