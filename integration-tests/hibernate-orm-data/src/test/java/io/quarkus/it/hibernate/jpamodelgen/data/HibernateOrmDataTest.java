package io.quarkus.it.hibernate.jpamodelgen.data;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class HibernateOrmDataTest {
    private static final String ROOT = "/data";

    @Test
    public void staticMetamodel() {
        // Create/retrieve
        given()
                .pathParam("name", "foo")
                .contentType(ContentType.JSON)
                .when().get(ROOT + "/by/name/{name}")
                .then()
                .statusCode(404);
        given()
                .contentType(ContentType.JSON)
                .when().get(ROOT)
                .then()
                .statusCode(200)
                .body(equalTo("[]"));
        given()
                .body(new MyEntity("foo"))
                .contentType(ContentType.JSON)
                .when().post(ROOT)
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "foo")
                .contentType(ContentType.JSON)
                .when().get(ROOT + "/by/name/{name}")
                .then()
                .statusCode(200);
        given()
                .contentType(ContentType.JSON)
                .when().get(ROOT)
                .then()
                .statusCode(200)
                .body(containsString("\"foo\""));

        // Update
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(ROOT + "/by/name/{name}")
                .then()
                .statusCode(404);
        given()
                .pathParam("before", "foo")
                .pathParam("after", "bar")
                .contentType(ContentType.JSON)
                .when().post(ROOT + "/rename/{before}/to/{after}")
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(ROOT + "/by/name/{name}")
                .then()
                .statusCode(200);

        // Delete
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().delete(ROOT + "/by/name/{name}")
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(ROOT + "/by/name/{name}")
                .then()
                .statusCode(404);
    }
}
