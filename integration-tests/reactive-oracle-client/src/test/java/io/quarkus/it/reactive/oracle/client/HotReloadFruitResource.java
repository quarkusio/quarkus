package io.quarkus.it.reactive.oracle.client;

import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.oracleclient.OraclePool;
import io.vertx.mutiny.sqlclient.Row;

@Path("/hot-fruits")
public class HotReloadFruitResource {

    @Inject
    OraclePool client;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE fruits").execute()
                .onFailure().recoverWithNull()
                .flatMap(r -> client.query("CREATE TABLE fruits (id INT PRIMARY KEY, name VARCHAR(500) NOT NULL)")
                        .execute())
                .flatMap(r -> client.query("INSERT INTO fruits (id, name) VALUES (1, 'Orange')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (id, name) VALUES (2, 'Pear')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (id, name) VALUES (3, 'Apple')").execute())
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
