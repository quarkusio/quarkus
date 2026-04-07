package io.quarkus.reactive.mysql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

public class ChangingCredentialsTestResource {

    @Inject
    Pool client;

    @Inject
    ChangingCredentialsProvider credentialsProvider;

    @Route(path = "/test", methods = Route.HttpMethod.GET)
    Uni<String> connect() {
        return client.query("SELECT CURRENT_USER").execute()
                .map(rowSet -> {
                    assertEquals(1, rowSet.size());
                    return rowSet.iterator().next().getString(0);
                }).eventually(credentialsProvider::changeProperties);
    }
}
