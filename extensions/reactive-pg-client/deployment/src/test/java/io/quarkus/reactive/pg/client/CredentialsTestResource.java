package io.quarkus.reactive.pg.client;

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
                .map(pgRowSet -> {
                    assertEquals(1, pgRowSet.size());
                    assertEquals(1, pgRowSet.iterator().next().getInteger(0));
                    return "OK";
                });
    }
}
