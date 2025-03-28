package io.quarkus.it.compose.devservices.rabbitmq;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(RabbitTestProfile.class)
public class RabbitmqTest {

    @Test
    public void test() {
        given()
                .when()
                .body("hello")
                .post("/amqp/send")
                .then()
                .statusCode(204);
        given()
                .when()
                .body("world")
                .post("/amqp/send")
                .then()
                .statusCode(204);

        await().untilAsserted(() -> given()
                .accept(ContentType.JSON)
                .when().get("/amqp/received")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("hello"),
                        Matchers.containsString("world")));
    }
}
