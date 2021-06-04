package io.quarkus.spring.cloud.config.client.oidc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
class GreetingResourceTest {

    @Test
    void testGreeting() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello from spring cloud config server"));
    }

}
