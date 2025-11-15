package io.quarkus.it.hibernate.processor.data;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.it.hibernate.processor.data.pudefault.MyEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class HibernateOrmDataTest {

    @ValueSource(strings = { "/data", "/data/other" })
    @ParameterizedTest
    public void testEntities(String root) {
        // Create/retrieve
        given()
                .pathParam("name", "foo")
                .contentType(ContentType.JSON)
                .when().get(root + "/by/name/{name}")
                .then()
                .statusCode(404);
        given()
                .contentType(ContentType.JSON)
                .when().get(root)
                .then()
                .statusCode(200)
                .body(equalTo("[]"));
        given()
                .body(new MyEntity("foo"))
                .contentType(ContentType.JSON)
                .when().post(root)
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "foo")
                .contentType(ContentType.JSON)
                .when().get(root + "/by/name/{name}")
                .then()
                .statusCode(200);
        given()
                .contentType(ContentType.JSON)
                .when().get(root)
                .then()
                .statusCode(200)
                .body(containsString("\"foo\""));

        // Update
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(root + "/by/name/{name}")
                .then()
                .statusCode(404);
        given()
                .pathParam("before", "foo")
                .pathParam("after", "bar")
                .contentType(ContentType.JSON)
                .when().post(root + "/rename/{before}/to/{after}")
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(root + "/by/name/{name}")
                .then()
                .statusCode(200);

        // Delete
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().delete(root + "/by/name/{name}")
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "bar")
                .contentType(ContentType.JSON)
                .when().get(root + "/by/name/{name}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testSqlOnly() {
        given()
                .pathParam("name", "admin")
                .contentType(ContentType.JSON)
                .when().get("/data/sqlonly/myuser/by/username/{name}")
                .then()
                .statusCode(404);
        given()
                .pathParam("id", 42)
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"role\":\"admin\"}")
                .when().put("/data/sqlonly/myuser/{id}")
                .then()
                .statusCode(204);
        given()
                .pathParam("name", "admin")
                .contentType(ContentType.JSON)
                .when().get("/data/sqlonly/myuser/by/username/{name}")
                .then()
                .statusCode(200)
                .body(equalTo("{\"id\":42,\"username\":\"admin\",\"role\":\"admin\"}"));
    }
}
