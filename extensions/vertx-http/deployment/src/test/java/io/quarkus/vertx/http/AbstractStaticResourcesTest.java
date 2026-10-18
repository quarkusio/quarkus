package io.quarkus.vertx.http;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractStaticResourcesTest {

    @Test
    public void shouldEncodeHtmlPage() {
        assertEncodedResponse("/static-file.html");
    }

    @Test
    public void shouldEncodeRootPage() {
        assertEncodedResponse("/");
    }

    @Test
    public void shouldEncodeHiddenHtmlPage() {
        assertEncodedResponse("/.hidden-file.html");
    }

    @Test
    public void shouldNotEncodeSVG() {
        RestAssured.when().get("/image.svg")
                .then()
                .header("Content-Encoding", Matchers.nullValue())
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }

    @Test
    public void shouldReturnRangeSupport() {
        RestAssured.when().head("/")
                .then()
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", Integer::parseInt, Matchers.greaterThan(0))
                .statusCode(200);
    }

    protected void assertEncodedResponse(String path) {
        RestAssured.when().get(path)
                .then()
                .header("Content-Encoding", "gzip")
                .header("Transfer-Encoding", "chunked")
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }

    @Test
    public void shouldGetFileWithSpecialCharacters() throws IOException {
        RestAssured.get("/l'équipe.pdf")
                .then()
                .header("Content-Type", Matchers.is("application/pdf"))
                .statusCode(200);
    }

    @Test
    public void shouldGetFileWithSpaces() throws IOException {
        RestAssured.get("/static file.txt")
                .then()
                .header("Content-Type", Matchers.is("text/plain;charset=UTF-8"))
                .statusCode(200);
    }

    @Test
    public void shouldGetFileWithSpacesAndQuery() throws IOException {
        RestAssured.get("/static file.txt?foo=bar")
                .then()
                .header("Content-Type", Matchers.is("text/plain;charset=UTF-8"))
                .statusCode(200);
    }

    @Test
    public void shouldWorkWithEncodedSlash() throws IOException {
        RestAssured.given().urlEncodingEnabled(false).get("/dir%2Ffile.txt")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldWorkWithDoubleDot() throws IOException {
        RestAssured.given().urlEncodingEnabled(false).get("/hello/../static-file.html")
                .then()
                .statusCode(200);
    }

}
