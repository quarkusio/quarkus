package io.quarkus.reactive.mysql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.mutiny.mysqlclient.MySQLPool;

@Path("/test")
public class CredentialsTestResource {

    @Inject
    MySQLPool client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> connect() {

        return client.query("SELECT 1").execute()
                .map(mysqlRowSet -> {
                    assertEquals(1, mysqlRowSet.size());
                    assertEquals(1, mysqlRowSet.iterator().next().getInteger(0));
                    return "OK";
                })
                .subscribeAsCompletionStage();
    }

}
