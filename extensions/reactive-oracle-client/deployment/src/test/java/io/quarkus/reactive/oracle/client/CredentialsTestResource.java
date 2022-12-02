package io.quarkus.reactive.oracle.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.mutiny.oracleclient.OraclePool;

@Path("/test")
public class CredentialsTestResource {

    @Inject
    OraclePool client;

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
