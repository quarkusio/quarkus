package io.quarkus.reactive.mysql.client;

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
        return client.query("SELECT 1").execute()
                .map(mysqlRowSet -> {
                    assertEquals(1, mysqlRowSet.size());
                    assertEquals(1, mysqlRowSet.iterator().next().getInteger(0));
                    return "OK";
                });
    }
}
