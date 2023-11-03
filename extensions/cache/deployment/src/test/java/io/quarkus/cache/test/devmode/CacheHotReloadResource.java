package io.quarkus.cache.test.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Path("/cache-hot-reload-test")
public class CacheHotReloadResource {

    private int invocations;

    @GET
    @Path("/greet")
    @CacheResult(cacheName = "hotReloadCache")
    public String greet(@QueryParam("key") String key) {
        invocations++;
        return "hello " + key + "!";
    }

    @GET
    @Path("/invocations")
    public int getInvocations() {
        return invocations;
    }
}
