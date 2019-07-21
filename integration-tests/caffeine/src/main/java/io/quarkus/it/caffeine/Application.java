package io.quarkus.it.caffeine;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Path("/test")
@ApplicationScoped
public class Application {
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .initialCapacity(16)
            .maximumSize(10_000)
            .removalListener((k, v, c) -> {
            })
            .build();

    @Path("/cache/{key}")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public void put(@PathParam("key") String key, String value) {
        cache.put(key, value);
    }

    @Path("/cache/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("key") String key) throws Exception {
        return cache.getIfPresent(key);
    }
}
