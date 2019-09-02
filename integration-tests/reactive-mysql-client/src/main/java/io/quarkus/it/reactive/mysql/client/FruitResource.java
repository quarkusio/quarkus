package io.quarkus.it.reactive.mysql.client;

import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.mysqlclient.MySQLPool;
import io.vertx.axle.sqlclient.Row;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/fruits")
public class FruitResource {

    @Inject
    MySQLPool client;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE IF EXISTS fruits")
                .thenCompose(r -> client.query("CREATE TABLE fruits (id SERIAL PRIMARY KEY, name TEXT NOT NULL)"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Orange')"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Pear')"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Apple')"))
                .toCompletableFuture()
                .join();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JsonArray> listFruits() {
        return client.query("SELECT * FROM fruits").thenApplyAsync(mysqlRowSet -> {
            JsonArray jsonArray = new JsonArray();
            for (Row row : mysqlRowSet) {
                jsonArray.add(toJson(row));
            }
            return jsonArray;
        });
    }

    private JsonObject toJson(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("name", row.getString("name"));
    }

}
