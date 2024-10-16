package io.quarkus.it.rabbitmq;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class RabbitMQDevServiceTopologyTest {

    @ConfigProperty(name = "rabbitmq-username")
    String username;

    @ConfigProperty(name = "rabbitmq-password")
    String password;

    @ConfigProperty(name = "rabbitmq-http-port")
    int rabbitMqHttpPort;

    @Test
    public void testVhosts() {
        RestAssured.given()
                .port(rabbitMqHttpPort)
                .auth().preemptive().basic(username, password)
                .when().get("/api/vhosts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", containsInAnyOrder("/", "my-vhost-1", "my-vhost-2"));
    }

    @Test
    public void testExchanges() {
        RestAssured.given()
                .port(rabbitMqHttpPort)
                .auth().preemptive().basic(username, password)
                .when().get("/api/exchanges/my-vhost-1/my-exchange-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("my-exchange-1"), "vhost", equalTo("my-vhost-1"));
    }

    @Test
    public void testQueues() {
        RestAssured.given()
                .port(rabbitMqHttpPort)
                .auth().preemptive().basic(username, password)
                .when().get("/api/queues/my-vhost-1/my-queue-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("my-queue-1"), "vhost", equalTo("my-vhost-1"));
    }

    @Test
    public void testBindings() {
        RestAssured.given()
                .port(rabbitMqHttpPort)
                .auth().preemptive().basic(username, password)
                .when().get("/api/bindings/my-vhost-1/e/my-exchange-1/q/my-queue-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("source[0]", equalTo("my-exchange-1"), "vhost[0]", equalTo("my-vhost-1"), "destination[0]",
                        equalTo("my-queue-1"));
    }
}
