package io.quarkus.reactive.oracle.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

public class CredentialsTestResource {

    @Inject
    Pool client;

    @Route(path = "/test", methods = Route.HttpMethod.GET)
    Uni<String> connect() {
        return client.query("SELECT 1 FROM DUAL").execute()
                .map(oracleRowSet -> {
                    assertEquals(1, oracleRowSet.size());
                    assertEquals(1, oracleRowSet.iterator().next().getInteger(0));
                    return "OK";
                });
    }
}
