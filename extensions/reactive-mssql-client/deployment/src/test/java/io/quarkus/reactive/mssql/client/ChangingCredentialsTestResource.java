package io.quarkus.reactive.mssql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

public class ChangingCredentialsTestResource {

    @Inject
    Pool client;

    @Inject
    ChangingCredentialsProvider credentialsProvider;

    void addUser(@Observes StartupEvent ignored) {
        client.query("CREATE LOGIN user2 WITH PASSWORD = 'yourStrong(!)Password2'").executeAndAwait();
        client.query("CREATE USER user2 FOR LOGIN user2").executeAndAwait();
    }

    @Route(path = "/test", methods = Route.HttpMethod.GET)
    Uni<String> connect() {
        return client.query("SELECT CURRENT_USER").execute()
                .map(rowSet -> {
                    assertEquals(1, rowSet.size());
                    return rowSet.iterator().next().getString(0);
                }).eventually(credentialsProvider::changeProperties);
    }
}
