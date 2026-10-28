package io.quarkus.it.cache;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;

@Path("/get-if-present")
public class GetIfPresentResource {

    public static final String GET_IF_PRESENT_CACHE_NAME = "getIfPresentCache";

    @CacheName(GET_IF_PRESENT_CACHE_NAME)
    Cache cache;

    public static final String PENDING_PUT_CACHE_NAME = "pendingPutCache";

    @CacheName(PENDING_PUT_CACHE_NAME)
    Cache pendingPutCache;

    @GET
    @Path("/{key}")
    public CompletionStage<String> getIfPresent(@RestPath String key) {
        return cache.as(CaffeineCache.class).getIfPresent(key);
    }

    @PUT
    @Path("/{key}")
    public void put(@RestPath String key, String value) {
        cache.as(CaffeineCache.class).put(key, completedFuture(value));
    }

    /**
     * Puts a value that is not available yet, and completes it after the put.
     */
    @PUT
    @Path("/pending/{key}")
    public void putPending(@RestPath String key, String value) {
        CompletableFuture<String> valueFuture = new CompletableFuture<>();
        pendingPutCache.as(CaffeineCache.class).put(key, valueFuture);
        valueFuture.complete(value);
    }
}
