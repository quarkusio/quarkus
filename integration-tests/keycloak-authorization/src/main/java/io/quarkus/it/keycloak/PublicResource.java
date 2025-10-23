package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;

@Path("/api")
public class PublicResource {

    @Inject
    ReactiveRedisDataSource redisDataSource;

    @Path("/token-state-count")
    @GET
    public int tokenStateCount() {
        return redisDataSource.execute("DBSIZE").await().indefinitely().toInteger();
    }

    @GET
    @Path("public")
    public void serve() {
        // no-op
    }

    @GET
    @Path("public-enforcing")
    public void serveEnforcing() {
        // no-op
    }

    @GET
    @Path("public-token")
    public void serveToken() {
        // no-op
    }
}
