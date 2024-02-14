package io.quarkus.it.hibernate.validator;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class TextResourceTest {

    @Test
    public void fetchDefault() {
        when().get("/text/validate/boom")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT);
    }

    @Test
    public void fetchText() {
        when().get("/text/validate/boom")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT);
    }

    @Test
    public void shouldGetAcceptLanguageLocaleIfKeyIsUpperCase() {
        given()
                .header("Accept-Language", "de")
                .when().get("/text/validate/boom")
                .then().log().ifValidationFails()
                .body(containsString("numerischer Wert außerhalb des gültigen Bereichs"));
    }

    @Test
    public void shouldGetAcceptLanguageLocaleIfKeyIsLowerCase() {
        given()
                .header("accept-language", "de")
                .when().get("/text/validate/boom")
                .then().log().ifValidationFails()
                .body(containsString("numerischer Wert außerhalb des gültigen Bereichs"));
    }
}
