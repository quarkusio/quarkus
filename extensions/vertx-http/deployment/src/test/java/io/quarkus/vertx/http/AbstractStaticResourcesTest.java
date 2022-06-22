package io.quarkus.vertx.http;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractStaticResourcesTest {

    @Test
    public void shouldEncodeHtmlPage() {
        RestAssured.when().get("/static-file.html")
                .then()
                .header("Content-Encoding", "gzip")
                .header("Transfer-Encoding", "chunked")
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }

    @Test
    public void shouldEncodeRootPage() {
        RestAssured.when().get("/")
                .then()
                .header("Content-Encoding", "gzip")
                .header("Transfer-Encoding", "chunked")
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }

    @Test
    public void shouldNotEncodeSVG() {
        RestAssured.when().get("/image.svg")
                .then()
                .header("Content-Encoding", Matchers.nullValue())
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }

}
