package io.quarkus.it.qute;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class QuteTestCase {

    @Test
    public void testTemplates() throws InterruptedException {
        RestAssured.when().get("/hello?name=Ciri").then().body(containsString("Hello Ciri!"));
        RestAssured.when().get("/hello").then().body(containsString("Hello world!"));
        RestAssured.given().accept(ContentType.HTML).when().get("/hello-route")
                .then()
                .contentType(is(ContentType.HTML.toString()))
                .body(containsString("Hello world!"));
        RestAssured.given().accept(ContentType.HTML).when().get("/hello-route?name=Ciri").then()
                .contentType(is(ContentType.HTML.toString()))
                .body(containsString("Hello Ciri!"));
        RestAssured.when().get("/beer").then().body(containsString("Beer Pilsner, completed: true, done: true"));
    }

    @Test
    public void testNotFoundPageStatusCode() {
        RestAssured.when().get("/not-exists").then()
                .body(containsString("Not Found!"))
                .statusCode(NOT_FOUND.getStatusCode());
    }
}
