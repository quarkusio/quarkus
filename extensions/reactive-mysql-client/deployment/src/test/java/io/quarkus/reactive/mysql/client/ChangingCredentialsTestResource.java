package io.quarkus.reactive.mysql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

@Path("/test")
public class ChangingCredentialsTestResource {

    @Inject
    Pool client;

    @Inject
    ChangingCredentialsProvider credentialsProvider;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> connect() {
        return client.query("SELECT CURRENT_USER").execute()
                .map(rowSet -> {
                    assertEquals(1, rowSet.size());
                    return Response.ok(rowSet.iterator().next().getString(0)).build();
                }).eventually(credentialsProvider::changeProperties);
    }

}
