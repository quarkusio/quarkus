package io.quarkus.it.rabbitmq;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class RabbitMQDevServiceTopologyTest {

    DevServicesContext context;

    public String getUsername() {
        return context.devServicesProperties().get("rabbitmq-username");
    }

    public String getPassword() {
        return context.devServicesProperties().get("rabbitmq-password");
    }

    public int getRabbitMqHttpPort() {
        return Integer.parseInt(context.devServicesProperties().get("rabbitmq-http-port"));
    }

    @Test
    public void testVhosts() {
        RestAssured.given()
                .port(getRabbitMqHttpPort())
                .auth().preemptive().basic(getUsername(), getPassword())
                .when().get("/api/vhosts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", containsInAnyOrder("/", "my-vhost-1", "my-vhost-2"));
    }

    @Test
    public void testExchanges() {
        RestAssured.given()
                .port(getRabbitMqHttpPort())
                .auth().preemptive().basic(getUsername(), getPassword())
                .when().get("/api/exchanges/my-vhost-1/my-exchange-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("my-exchange-1"), "vhost", equalTo("my-vhost-1"));
    }

    @Test
    public void testQueues() {
        RestAssured.given()
                .port(getRabbitMqHttpPort())
                .auth().preemptive().basic(getUsername(), getPassword())
                .when().get("/api/queues/my-vhost-1/my-queue-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("my-queue-1"), "vhost", equalTo("my-vhost-1"));
    }

    @Test
    public void testBindings() {
        RestAssured.given()
                .port(getRabbitMqHttpPort())
                .auth().preemptive().basic(getUsername(), getPassword())
                .when().get("/api/bindings/my-vhost-1/e/my-exchange-1/q/my-queue-1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("source[0]", equalTo("my-exchange-1"), "vhost[0]", equalTo("my-vhost-1"), "destination[0]",
                        equalTo("my-queue-1"));
    }
}
