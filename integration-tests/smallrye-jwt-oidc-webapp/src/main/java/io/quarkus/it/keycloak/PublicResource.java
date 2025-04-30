package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.mutiny.sqlclient.Pool;

@Path("/public")
public class PublicResource {

    @Inject
    Pool pool;

    @Path("/token-state-count")
    @GET
    public int tokenStateCount() {
        return pool
                .query("SELECT COUNT(*) FROM oidc_db_token_state_manager")
                .execute()
                .map(rs -> rs.iterator().next().getInteger(0))
                .await()
                .indefinitely();
    }

}
