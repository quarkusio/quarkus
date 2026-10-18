package io.quarkus.it.cache.multiplebackends;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;

// Helper resource to get the cache manager implementations used by the application.
@ApplicationScoped
@Path("/cache-managers")
public class CacheManagerResource {

    @Inject
    private CacheManager cacheManager;

    @GET
    @Path("/")
    public Map<String, String> getCacheManagers() {
        Map<String, String> cacheManagerClasses = new HashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            cacheManagerClasses.put(cacheName, cacheManager.getCache(cacheName).orElseThrow().getClass().getName());
        }
        return cacheManagerClasses;
    }

    @GET
    @Path("/{cacheName}/keys/{key}")
    public String getCacheValueByKey(@PathParam("cacheName") String cacheName, @PathParam("key") String key) {
        Cache cache = cacheManager.getCache(cacheName).orElseThrow();
        return cache.get(key, defaultValue -> "").await().indefinitely();
    }
}
