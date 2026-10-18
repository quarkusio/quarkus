package io.quarkus.cache.runtime.dev.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class CacheJsonRPCService {

    @Inject
    CacheManager manager;

    @NonBlocking
    @JsonRpcDescription("Get all available caches (name and size)")
    public JsonArray getAll() {
        Collection<String> names = manager.getCacheNames();
        List<CaffeineCache> allCaches = new ArrayList<>(names.size());
        for (String name : names) {
            Optional<Cache> cache = manager.getCache(name);
            if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
                allCaches.add((CaffeineCache) cache.get());
            }
        }
        allCaches.sort(Comparator.comparing(CaffeineCache::getName));

        var array = new JsonArray();
        for (CaffeineCache cc : allCaches) {
            array.add(getJsonRepresentationForCache(cc));
        }
        return array;
    }

    private JsonObject getJsonRepresentationForCache(Cache cc) {
        return new JsonObject().put("name", cc.getName()).put("size", ((CaffeineCacheImpl) cc).getSize());
    }

    @JsonRpcDescription("Clear a specific cache")
    public Uni<JsonObject> clear(@JsonRpcDescription("The cache name") String name) {
        Optional<Cache> cache = manager.getCache(name);
        if (cache.isPresent()) {
            return cache.get().invalidateAll().map((t) -> getJsonRepresentationForCache(cache.get()));
        } else {
            return Uni.createFrom().item(new JsonObject().put("name", name).put("size", -1));
        }
    }

    @NonBlocking
    public JsonObject refresh(String name) {
        Optional<Cache> cache = manager.getCache(name);
        if (cache.isPresent()) {
            return getJsonRepresentationForCache(cache.get());
        } else {
            return new JsonObject().put("name", name).put("size", -1);
        }
    }

    @JsonRpcDescription("The all the keys in a specific cache as a String array")
    public JsonArray getKeys(@JsonRpcDescription("The cache name") String name) {
        Optional<Cache> cache = manager.getCache(name);
        if (cache.isPresent()) {
            CaffeineCache caffeineCache = (CaffeineCache) cache.get();
            JsonArray keys = new JsonArray();
            for (Object key : caffeineCache.keySet()) {
                keys.add(key.toString());
            }
            return keys;
        } else {
            return JsonArray.of();
        }
    }

}
