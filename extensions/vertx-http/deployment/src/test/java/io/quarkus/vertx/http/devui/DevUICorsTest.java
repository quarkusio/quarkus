package io.quarkus.vertx.http.devui;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevUICorsTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    @Test
    public void testPreflightHttpLocalhostOrigin() {
        String origin = "http://localhost:8080";
        String methods = "GET,POST";
        RestAssured.given().header("Origin", origin).header("Access-Control-Request-Method", methods).when()
                .options("q/dev-ui/configuration-form-editor").then().statusCode(200)
                .header("Access-Control-Allow-Origin", origin).header("Access-Control-Allow-Methods", methods)
                .body(emptyOrNullString());
    }

    @Test
    public void testPreflightHttpLocalhostIpOrigin() {
        String origin = "http://127.0.0.1:8080";
        String methods = "GET,POST";
        RestAssured.given().header("Origin", origin).header("Access-Control-Request-Method", methods).when()
                .options("q/dev-ui/configuration-form-editor").then().statusCode(200)
                .header("Access-Control-Allow-Origin", origin).header("Access-Control-Allow-Methods", methods)
                .body(emptyOrNullString());
    }

    @Test
    public void testPreflightHttpsLocalhostOrigin() {
        String origin = "https://localhost:8443";
        String methods = "GET,POST";
        RestAssured.given().header("Origin", origin).header("Access-Control-Request-Method", methods).when()
                .options("q/dev-ui/configuration-form-editor").then().statusCode(200)
                .header("Access-Control-Allow-Origin", origin).header("Access-Control-Allow-Methods", methods)
                .body(emptyOrNullString());
    }

    @Test
    public void testPreflightHttpsLocalhostIpOrigin() {
        String origin = "https://127.0.0.1:8443";
        String methods = "GET,POST";
        RestAssured.given().header("Origin", origin).header("Access-Control-Request-Method", methods).when()
                .options("q/dev-ui/configuration-form-editor").then().statusCode(200)
                .header("Access-Control-Allow-Origin", origin).header("Access-Control-Allow-Methods", methods)
                .body(emptyOrNullString());
    }

    @Test
    public void testPreflightNonLocalhostOrigin() {
        String methods = "GET,POST";
        RestAssured.given().header("Origin", "https://quarkus.io/http://localhost")
                .header("Access-Control-Request-Method", methods).when().options("q/dev-ui/configuration-form-editor")
                .then().statusCode(403).header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Methods", nullValue()).body(emptyOrNullString());
    }

    @Test
    public void testPreflightBadLocalhostOrigin() {
        String methods = "GET,POST";
        RestAssured.given().header("Origin", "http://localhost:8080/devui")
                .header("Access-Control-Request-Method", methods).when().options("q/dev-ui/configuration-form-editor")
                .then().statusCode(403).header("Access-Control-Allow-Origin", nullValue()).body(emptyOrNullString());
    }

    @Test
    public void testPreflightBadLocalhostIpOrigin() {
        String methods = "GET,POST";
        RestAssured.given().header("Origin", "http://127.0.0.1:8080/devui")
                .header("Access-Control-Request-Method", methods).when().options("q/dev-ui/configuration-form-editor")
                .then().statusCode(403).header("Access-Control-Allow-Origin", nullValue()).body(emptyOrNullString());
    }

    @Test
    public void testPreflightLocalhostOriginWithoutPort() {
        String methods = "GET,POST";
        RestAssured.given().header("Origin", "http://localhost").header("Access-Control-Request-Method", methods).when()
                .options("q/dev-ui/configuration-form-editor").then().statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue()).body(emptyOrNullString());
    }

    @Test
    public void testSimpleRequestHttpLocalhostOrigin() {
        String origin = "http://localhost:8080";
        RestAssured.given().header("Origin", origin).when().get("q/dev-ui/configuration-form-editor").then()
                .statusCode(200).header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", nullValue()).body(not(emptyOrNullString()));
    }

    @Test
    public void testSimpleRequestHttpLocalhostIpOrigin() {
        String origin = "http://127.0.0.1:8080";
        RestAssured.given().header("Origin", origin).when().get("q/dev-ui/configuration-form-editor").then()
                .statusCode(200).header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", nullValue()).body(not(emptyOrNullString()));
    }

    @Test
    public void testSimpleRequestHttpsLocalhostOrigin() {
        String origin = "https://localhost:8443";
        RestAssured.given().header("Origin", origin).when().get("q/dev-ui/configuration-form-editor").then()
                .statusCode(200).header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", nullValue()).body(not(emptyOrNullString()));
    }

    @Test
    public void testSimpleRequestHttpsLocalhostIpOrigin() {
        String origin = "https://127.0.0.1:8443";
        RestAssured.given().header("Origin", origin).when().get("q/dev-ui/configuration-form-editor").then()
                .statusCode(200).header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", nullValue()).body(not(emptyOrNullString()));
    }

    @Test
    public void testSimpleRequestNonLocalhostOrigin() {
        RestAssured.given().header("Origin", "https://quarkus.io/http://localhost").when()
                .get("q/dev-ui/configuration-form-editor").then().statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue()).body(emptyOrNullString());
    }
}
