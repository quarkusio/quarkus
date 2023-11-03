package io.quarkus.flyway.test;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

// see https://github.com/quarkusio/quarkus/issues/9415
public class FlywayMultiDataSourcesDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultiDataSourcesDevModeEndpoint.class, FlywayExtensionCallback.class)
                    .addAsResource("config-for-multiple-datasources.properties", "application.properties"));

    @Test
    public void testProperConfigApplied() {
        RestAssured.get("/fly").then()
                .statusCode(200)
                .body(containsString("db/location1,db/location2"));

        RestAssured.get("/fly?name=users").then()
                .statusCode(200)
                .body(containsString("db/users/location1,db/users/location2"));

        RestAssured.get("/fly?name=inventory").then()
                .statusCode(200)
                .body(containsString("db/inventory/location1,db/inventory/location"));
    }

}
