package io.quarkus.it.cache.infinispan;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

@RegisterRestClient
@Path("sunrise")
public interface SunriseRestClient {

    String CACHE_NAME = "sunrise-cache";

    @GET
    @Path("time/{city}")
    @CacheResult(cacheName = CACHE_NAME)
    String getSunriseTime(@RestPath String city, @RestQuery String date);

    @GET
    @Path("time/{city}")
    @CacheResult(cacheName = CACHE_NAME)
    Uni<String> getAsyncSunriseTime(@RestPath String city, @RestQuery String date);

    @GET
    @Path("invocations")
    Integer getSunriseTimeInvocations();

    /*
     * The following methods wouldn't make sense in a real-life application but it's not relevant here. We only need to check if
     * the caching annotations work as intended with the rest-client extension.
     */

    @DELETE
    @Path("invalidate/{city}")
    @CacheInvalidate(cacheName = CACHE_NAME)
    Uni<Void> invalidate(@CacheKey @RestPath String city, @RestQuery String notPartOfTheCacheKey,
            @CacheKey @RestPath String date);

    @DELETE
    @Path("invalidate")
    @CacheInvalidateAll(cacheName = CACHE_NAME)
    void invalidateAll();
}
