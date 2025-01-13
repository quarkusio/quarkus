package io.quarkus.mongodb;

import static io.restassured.RestAssured.when;

import jakarta.inject.Inject;

import org.bson.Document;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.MongoClientException;
import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NoConnectionHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.connection-string", "mongodb://localhost:9999")
            // timeouts set to the test doesn't take too long to run
            .overrideConfigKey("quarkus.mongodb.connect-timeout", "2s")
            .overrideConfigKey("quarkus.mongodb.server-selection-timeout", "2s")
            .overrideConfigKey("quarkus.mongodb.read-timeout", "2s");

    @Inject
    MongoClient mongo;

    @Order(1) // done to ensure the health check runs before any application code touches the database
    @Test
    public void healthCheck() {
        when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("DOWN"));
    }

    @Order(2)
    @Test
    public void tryConnection() {
        Assertions.assertThrows(MongoClientException.class, () -> {
            mongo.getDatabase("admin").runCommand(new Document("ping", 1));
        });
    }

}
