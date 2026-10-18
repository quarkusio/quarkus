package io.quarkus.it.cache.multiplebackends;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Path("/caches")
public class MultipleBackendResource {
    public static final String REDIS_CACHE_NAME = "redis-cache";
    public static final String CAFFEINE_CACHE_NAME = "caffeine-cache";

    @GET
    @Path("/redis/{key}")
    @CacheResult(cacheName = REDIS_CACHE_NAME)
    public String getValueAndCacheItOnRedis(@PathParam("key") @CacheKey String key) {
        return UUID.randomUUID().toString();
    }

    @DELETE
    @Path("/redis")
    @CacheInvalidateAll(cacheName = REDIS_CACHE_NAME)
    public Response invalidateAllCacheOnRedis() {
        return Response.noContent().build();
    }

    @GET
    @Path("/caffeine/{key}")
    @CacheResult(cacheName = CAFFEINE_CACHE_NAME)
    public String getValueAndCacheItOnCaffeine(@PathParam("key") @CacheKey String key) {
        return UUID.randomUUID().toString();
    }

    @DELETE
    @Path("/caffeine")
    @CacheInvalidateAll(cacheName = CAFFEINE_CACHE_NAME)
    public Response invalidateAllCacheOnCaffeine() {
        return Response.noContent().build();
    }
}
