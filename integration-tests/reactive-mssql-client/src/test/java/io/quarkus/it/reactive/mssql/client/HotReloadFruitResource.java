package io.quarkus.it.reactive.mssql.client;

import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mssqlclient.MSSQLPool;
import io.vertx.mutiny.sqlclient.Row;

@Path("/hot-fruits")
public class HotReloadFruitResource {

    @Inject
    MSSQLPool client;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE IF EXISTS fruits").execute()
                .flatMap(r -> client.query("CREATE TABLE fruits (id BIGINT IDENTITY PRIMARY KEY, name VARCHAR(500) NOT NULL)")
                        .execute())
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
