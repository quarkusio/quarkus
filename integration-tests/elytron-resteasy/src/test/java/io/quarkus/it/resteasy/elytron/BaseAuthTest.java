package io.quarkus.it.resteasy.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class BaseAuthTest {

    @Test
    void testPost() {
        // This is a regression test in that we had a problem where the Vert.x request was not paused
        // before the authentication filters ran and the post message was thrown away by Vert.x because
        // RESTEasy hadn't registered its request handlers yet.
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .body("{\"traveller\" : {\"firstName\" : \"John\",\"lastName\" : \"Doe\",\"email\" : \"john.doe@example.com\",\"nationality\" : \"American\",\"address\" : {\"street\" : \"main street\",\"city\" : \"Boston\",\"zipCode\" : \"10005\",\"country\" : \"US\"}}}")
                .contentType(ContentType.TEXT)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .body(is("post success"));
    }

    @Test
    void testGet() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .body(is("get success"));
    }

}
