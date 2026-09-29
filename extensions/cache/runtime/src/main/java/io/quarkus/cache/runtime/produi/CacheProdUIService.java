package io.quarkus.cache.runtime.produi;

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
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class CacheProdUIService {

    @Inject
    CacheManager manager;

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Get all available caches with name and size")
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
            array.add(new JsonObject()
                    .put("name", cc.getName())
                    .put("size", ((CaffeineCacheImpl) cc).getSize()));
        }
        return array;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Get the number of caches")
    public int count() {
        return manager.getCacheNames().size();
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Refresh cache info for a specific cache")
    public JsonObject refresh(@JsonRpcDescription("The cache name") String name) {
        Optional<Cache> cache = manager.getCache(name);
        if (cache.isPresent()) {
            return new JsonObject()
                    .put("name", cache.get().getName())
                    .put("size", ((CaffeineCacheImpl) cache.get()).getSize());
        }
        return new JsonObject().put("name", name).put("size", -1);
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Get all keys in a specific cache")
    public JsonArray getKeys(@JsonRpcDescription("The cache name") String name) {
        Optional<Cache> cache = manager.getCache(name);
        if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache.get();
            JsonArray keys = new JsonArray();
            for (Object key : caffeineCache.keySet()) {
                keys.add(key.toString());
            }
            return keys;
        }
        return new JsonArray();
    }
}
