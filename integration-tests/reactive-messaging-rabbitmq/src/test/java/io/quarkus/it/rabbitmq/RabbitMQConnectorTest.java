package io.quarkus.it.rabbitmq;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

@QuarkusTest
public class RabbitMQConnectorTest {

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void testHealthCheck() {
        RestAssured.when().get("/q/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP", "UP", "UP"),
                        "checks.name", containsInAnyOrder("SmallRye Reactive Messaging - liveness check",
                                "SmallRye Reactive Messaging - readiness check",
                                "SmallRye Reactive Messaging - startup check"),
                        "checks.data", not(containsString("people-in")));
    }

    @Test
    public void test() {
        await().atMost(30, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(6, get("/rabbitmq/people").as(TYPE_REF).size()));
    }

}
