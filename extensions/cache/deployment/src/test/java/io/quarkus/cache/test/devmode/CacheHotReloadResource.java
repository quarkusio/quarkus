package io.quarkus.cache.test.devmode;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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
