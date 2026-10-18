package io.quarkus.it.cache;

import static java.util.concurrent.CompletableFuture.completedFuture;

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
}
