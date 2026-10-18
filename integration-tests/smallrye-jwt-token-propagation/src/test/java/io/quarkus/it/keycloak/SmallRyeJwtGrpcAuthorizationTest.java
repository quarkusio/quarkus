package io.quarkus.it.keycloak;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class SmallRyeJwtGrpcAuthorizationTest {

    final KeycloakTestClient client = new KeycloakTestClient();

    @Test
    public void test() {
        RestAssured.given().auth().oauth2(client.getAccessToken("john"))
                .when().get("/hello/admin")
                .then()
                .statusCode(500);
        RestAssured.given().auth().oauth2(client.getAccessToken("john"))
                .when().get("/hello/tester")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Severus from john"));
    }
}
