package io.quarkus.reactive.h2.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.mutiny.jdbcclient.JDBCPool;

@Path("/test")
public class CredentialsTestResource {

    @Inject
    JDBCPool client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> connect() {

        return client.query("SELECT 1").execute()
                .map(h2RowSet -> {
                    assertEquals(1, h2RowSet.size());
                    assertEquals(1, h2RowSet.iterator().next().getInteger(0));
                    return "OK";
                })
                .subscribeAsCompletionStage();
    }
}
