package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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

    @GET
    @Path("contains-key/{tenantId}/{token}")
    public boolean containsKey(@PathParam("tenantId") String tenantId,
            @PathParam("token") String token) {
        return tokenCache.containsCacheKey(tenantId, token);
    }
}
