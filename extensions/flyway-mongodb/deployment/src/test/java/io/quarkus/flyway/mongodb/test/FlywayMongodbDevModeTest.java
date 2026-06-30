package io.quarkus.flyway.mongodb.test;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Verifies that when no MongoDB connection string is configured the app still boots
 * (Flyway bean is inactive), and that adding a valid connection string makes it active.
 */
@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MongodbDevModeTestEndpoint.class)
                    .addAsResource("db/migration/V1__create_users.json")
                    .addAsResource("config-empty-mongodb.properties", "application.properties"));

    @Test
    @DisplayName("Injecting Flyway MongoDB should fail if there is no MongoDB connection string configured")
    public void testAddingFlywayMongodb() {
        RestAssured.get("/fly").then().statusCode(500);
        config.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "quarkus.mongodb.connection-string=" + FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING + "\n"
                        + "quarkus.mongodb.database=devmodetest\n"
                        + "quarkus.flyway-mongodb.migrate-at-start=true\n"
                        + "quarkus.flyway-mongodb.database=devmodetest\n"
                        + "quarkus.flyway-mongodb.migration-suffixes=.json";
            }
        });
        RestAssured.get("/fly").then().statusCode(200);
    }
}
