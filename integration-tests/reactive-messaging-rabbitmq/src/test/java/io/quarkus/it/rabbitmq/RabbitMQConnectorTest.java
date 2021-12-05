package io.quarkus.it.rabbitmq;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class RabbitMQConnectorTest {

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void test() {
        await().atMost(30, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(6, get("/rabbitmq/people").as(TYPE_REF).size()));
    }

}
