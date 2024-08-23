package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("cache")
public class CacheResource {

    @Inject
    CustomIntrospectionUserInfoCache tokenCache;

    @POST
    @Path("clear")
    public int clear() {
        tokenCache.clearCache();
        return tokenCache.getCacheSize();
    }

    @GET
    @Path("size")
    public int size() {
        return tokenCache.getCacheSize();
    }
}
