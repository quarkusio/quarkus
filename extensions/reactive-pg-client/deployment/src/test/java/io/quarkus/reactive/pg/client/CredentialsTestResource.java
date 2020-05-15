package io.quarkus.reactive.pg.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.mutiny.pgclient.PgPool;

@Path("/test")
public class CredentialsTestResource {

    @Inject
    PgPool client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> connect() {

        return client.query("SELECT 1").execute()
                .map(pgRowSet -> {
                    assertEquals(1, pgRowSet.size());
                    assertEquals(1, pgRowSet.iterator().next().getInteger(0));
                    return "OK";
                })
                .subscribeAsCompletionStage();
    }

}
