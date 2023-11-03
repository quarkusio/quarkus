package io.quarkus.flyway.test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import db.migration.V1_0_1__Update;
import db.migration.V1_0_2__Update;
import io.quarkus.test.QuarkusDevModeTest;

public class FlywayExtensionWithJavaMigrationDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(V1_0_1__Update.class, V1_0_2__Update.class,
                            FlywayExtensionWithJavaMigrationDevModeTestEndpoint.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("clean-and-migrate-at-start-config.properties", "application.properties"));

    @Test
    public void test() throws SQLException {
        get("/fly")
                .then()
                .statusCode(200)
                .body(is("2/1.0.2"));

        config.modifySourceFile(FlywayExtensionWithJavaMigrationDevModeTestEndpoint.class, s -> s.replace("/fly", "/flyway"));

        get("/flyway")
                .then()
                .statusCode(200)
                .body(is("2/1.0.2"));
    }

}
