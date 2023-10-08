package io.quarkus.it.compose.devservices.redis;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(RedisTestProfile.class)
public class RedisServiceTest {

    @Test
    public void testKeys() {
        given()
                .contentType(ContentType.TEXT)
                .body("foo")
                .when()
                .put("/redis/test")
                .then()
                .statusCode(204);

        given()
                .accept(ContentType.TEXT)
                .when()
                .get("/redis/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foo"));
    }

    @Test
    public void testConnection() {
        given()
                .when()
                .get("/redis/host")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.emptyOrNullString()));

        given()
                .accept(ContentType.TEXT)
                .when()
                .get("/redis/password")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("qwerty"));
    }

}
