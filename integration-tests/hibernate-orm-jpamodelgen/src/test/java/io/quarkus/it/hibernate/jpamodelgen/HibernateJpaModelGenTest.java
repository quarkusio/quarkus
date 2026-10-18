package io.quarkus.it.hibernate.jpamodelgen;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class HibernateJpaModelGenTest {
    private static final String ROOT = "/static-metamodel";

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
                .body(new MyStaticMetamodelEntity("foo"))
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
