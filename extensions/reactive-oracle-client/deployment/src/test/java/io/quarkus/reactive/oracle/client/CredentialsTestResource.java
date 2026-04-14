package io.quarkus.reactive.oracle.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.mutiny.sqlclient.Pool;

@Path("/test")
public class CredentialsTestResource {

    @Inject
    Pool client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> connect() {

        return client.query("SELECT 1 FROM DUAL").execute()
                .map(oracleRowSet -> {
                    assertEquals(1, oracleRowSet.size());
                    assertEquals(1, oracleRowSet.iterator().next().getInteger(0));
                    return "OK";
                })
                .subscribeAsCompletionStage();
    }

}
