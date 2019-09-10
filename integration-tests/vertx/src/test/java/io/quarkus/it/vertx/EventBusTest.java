package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class EventBusTest {

    @Test
    public void testEventBusWithString() {
        String body = new JsonObject().put("name", "Bob Morane").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/vertx-test/event-bus/person")
                .then().statusCode(200).body(equalTo("Hello Bob Morane"));
    }

    @Test
    public void testEventBusWithPet() {
        String body = new JsonObject().put("name", "Neo").put("kind", "rabbit").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/vertx-test/event-bus/pet")
                .then().statusCode(200).body(equalTo("Hello Neo (rabbit)"));
    }
}
