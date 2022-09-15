package io.quarkus.it.reactive.pg.client;

import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;

@Path("/fruits")
public class FruitResource {

    @Inject
    PgPool client;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE IF EXISTS fruits").execute()
                .flatMap(r -> client.query("CREATE TABLE fruits (id SERIAL PRIMARY KEY, name TEXT NOT NULL)").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Orange')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Pear')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Apple')").execute())
                .await().indefinitely();
    }

    @GET
    public CompletionStage<JsonArray> listFruits() {
        return client.query("SELECT * FROM fruits").execute()
                .map(pgRowSet -> {
                    JsonArray jsonArray = new JsonArray();
                    for (Row row : pgRowSet) {
                        jsonArray.add(toJson(row));
                    }
                    return jsonArray;
                })
                .subscribeAsCompletionStage();
    }

    private JsonObject toJson(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("name", row.getString("name"));
    }

}
