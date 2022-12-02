package io.quarkus.resteasy.jsonb;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Verify that the Vert.x JsonObject / JsonArray serializers are registered.
 */
public class VertxSerializerRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceSendingJsonObjects.class));

    @Test
    public void testSerializationOfObjects() {
        RestAssured
                .get("/test/objects")
                .then()
                .statusCode(200)
                .body("[0].name", is("bob"))
                .body("[0].kind", is("cat"))
                .body("[1].name", is("titi"))
                .body("[1].kind", is("bird"));
    }

    @Test
    public void testSerializationOfArrays() {
        RestAssured
                .get("/test/arrays")
                .then()
                .statusCode(200)
                .body("[0].name", contains("bob", "titi"))
                .body("[0].kind", contains("cat", "bird"));
    }

    @Test
    public void testSendingJsonObjects() {
        RestAssured
                .given()
                .body(new JsonArray()
                        .add(new JsonObject().put("name", "bob").put("kind", "cat"))
                        .add(new JsonObject().put("name", "titi").put("kind", "bird"))
                        .encode())
                .header("Content-Type", "application/json")
                .post("/test/objects")
                .then().statusCode(204);
    }

    @Test
    public void testSendingJsonArrays() {
        RestAssured
                .given()
                .body(new JsonArray()
                        .add(new JsonArray()
                                .add(new JsonObject().put("name", "bob").put("kind", "cat"))
                                .add(new JsonObject().put("name", "titi").put("kind", "bird")))
                        .encode())
                .header("Content-Type", "application/json")
                .post("/test/arrays")
                .then().statusCode(204);
    }

}
