package io.quarkus.it.camel.servlet;

import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CamelServletTest {

    @Test
    public void multiplePaths() throws Throwable {
        RestAssured.when().get("/folder-1/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().get("/folder-2/rest-get").then().body(IsEqual.equalTo("GET: /rest-get"));
        RestAssured.when().post("/folder-1/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().post("/folder-2/rest-post").then().body(IsEqual.equalTo("POST: /rest-post"));
        RestAssured.when().get("/folder-1/hello").then().body(IsEqual.equalTo("GET: /hello"));
        RestAssured.when().get("/folder-2/hello").then().body(IsEqual.equalTo("GET: /hello"));
    }

    @Test
    public void namedWithservletClass() throws Throwable {
        RestAssured.when().get("/my-named-folder/custom").then()
                .body(IsEqual.equalTo("GET: /custom"))
                .and().header("x-servlet-class-name", CustomServlet.class.getName());
    }

    @Test
    public void ignoredKey() throws Throwable {
        RestAssured.when().get("/my-favorite-folder/favorite").then()
                .body(IsEqual.equalTo("GET: /favorite"));
    }
}
