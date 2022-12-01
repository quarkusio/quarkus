package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.annotations.cache.NoCache;

@Path("/")
public class CacheResource {

    @GET
    @Path("/nocache")
    @NoCache(fields = { "foo" })
    public String noCache() {
        return "No Cache";
    }

    @GET
    @Path("/cache")
    @Cache(maxAge = 123)
    public String cache() {
        return "Cache";
    }

}
