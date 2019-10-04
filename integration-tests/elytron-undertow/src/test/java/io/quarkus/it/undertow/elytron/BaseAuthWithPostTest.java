package io.quarkus.it.undertow.elytron;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class BaseAuthWithPostTest {

    @Test
    void testPost() {
        // This is a regression test in that we had a problem where the vertx request was not paused
        // before the authentication filters ran and the post message was thrown away by vertx because
        // resteasy hadn't registered its request handlers yet.
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .body("Bill")
                .contentType(ContentType.TEXT)
                .when()
                .post("/")
                .then()
                .statusCode(200);
    }

    @Test
    void testGet() {
        String output = given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/")
                .then()
                .statusCode(200).extract().asString();
    }

}
