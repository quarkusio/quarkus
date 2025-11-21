package io.quarkus.it.reactive.db2.client;

import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;

@Path("/plants")
public class PlantResource {

    @Inject
    Pool client;

    @Inject
    @ReactiveDataSource("extra")
    Pool additionalClient;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE IF EXISTS fruits").execute()
                .flatMap(r -> client
                        .query("CREATE TABLE fruits (id INTEGER NOT NULL GENERATED AS IDENTITY, name VARCHAR(50) NOT NULL)")
                        .execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Orange')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Pear')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Apple')").execute())
                .await().indefinitely();

        System.out.println("DEBUG setupDb");
        additionalClient.query("DROP TABLE IF EXISTS vegetables").execute()
                .flatMap(r -> client
                        .query("CREATE TABLE veg (id INTEGER NOT NULL GENERATED AS IDENTITY, name VARCHAR(50) NOT NULL)")
                        .execute())
                .flatMap(r -> client.query("INSERT INTO veg (name) VALUES ('Cumcumber')").execute())
                .flatMap(r -> client.query("INSERT INTO veg (name) VALUES ('Broccoli')").execute())
                .flatMap(r -> client.query("INSERT INTO veg (name) VALUES ('Leeks')").execute())
                .await().indefinitely();
        System.out.println("DEBUG done setupDb");
        additionalClient.query("SELECT * FROM veg ORDER BY name").execute()
                .map(rowSet -> {
                    JsonArray jsonArray = new JsonArray();
                    for (Row row : rowSet) {
                        jsonArray.add(toJson(row));
                    }
                    return jsonArray;
                }).invoke(a -> System.out.println("DEBUG db contents " + a.encodePrettily()));

    }

    @GET
    @Path("/fruits/")
    public CompletionStage<JsonArray> listFruits() {
        return client.query("SELECT * FROM fruits ORDER BY name").execute()
                .map(rowSet -> {
                    JsonArray jsonArray = new JsonArray();
                    for (Row row : rowSet) {
                        jsonArray.add(toJson(row));
                    }
                    return jsonArray;
                })
                .subscribeAsCompletionStage();
    }

    @GET
    @Path("/veg/")
    public CompletionStage<JsonArray> listveg() {
        return additionalClient.query("SELECT * FROM veg ORDER BY name").execute()
                .map(rowSet -> {
                    JsonArray jsonArray = new JsonArray();
                    for (Row row : rowSet) {
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
