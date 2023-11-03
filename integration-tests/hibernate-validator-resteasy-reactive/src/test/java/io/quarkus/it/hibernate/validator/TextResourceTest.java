package io.quarkus.it.hibernate.validator;

import static io.restassured.RestAssured.when;

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
}
