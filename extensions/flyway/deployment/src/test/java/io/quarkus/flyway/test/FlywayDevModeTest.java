package io.quarkus.flyway.test;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Flyway needs a datasource to work.
 * This tests assures, that an error occurs,
 * as soon as the default flyway configuration points to an missing default datasource.
 */
public class FlywayDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DevModeTestEndpoint.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("config-empty.properties", "application.properties"));

    @Test
    @DisplayName("Injecting (default) flyway should fail if there is no datasource configured")
    public void testAddingFlyway() {
        RestAssured.get("fly").then().statusCode(500);
        config.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "quarkus.datasource.db-kind=h2\n" +
                        "quarkus.datasource.username=sa\n" +
                        "quarkus.datasource.password=sa\n" +
                        "quarkus.datasource.jdbc.url=jdbc:h2:tcp://localhost/mem:test-quarkus-dev-mode;DB_CLOSE_DELAY=-1\n" +
                        "quarkus.flyway.migrate-at-start=true";
            }
        });
        RestAssured.get("/fly").then().statusCode(200);

    }
}
