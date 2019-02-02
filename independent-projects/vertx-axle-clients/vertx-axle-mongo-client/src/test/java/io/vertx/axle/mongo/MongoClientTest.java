package io.vertx.axle.mongo;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.mongo.MongoClient;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("mongo")
            .withExposedPorts(27017);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        MongoClient client = MongoClient.createShared(vertx, new JsonObject()
                .put("db_name", "axle-test")
                .put("connection_string", "mongodb://" + container.getContainerIpAddress()
                        + ":" + container.getMappedPort(27017)));

        JsonObject document = new JsonObject().put("title", "The Hobbit");
        List<JsonObject> list = client.save("books", document)
                .thenCompose(x -> client.find("books", new JsonObject().put("title", "The Hobbit")))
                .toCompletableFuture()
                .join();

        assertThat(list).hasSize(1)
                .allMatch(json -> json.getString("title").equalsIgnoreCase("The Hobbit"));
    }
}
