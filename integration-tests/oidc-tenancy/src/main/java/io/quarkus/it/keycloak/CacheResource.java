package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
