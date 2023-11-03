package io.quarkus.it.mqtt;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.post;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MQTTConnectorTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    public void test() {
        post("/mqtt/people");

        await().atMost(30, SECONDS)
                .untilAsserted(() -> Assertions.assertEquals(6, get("/mqtt/people").as(TYPE_REF).size()));
    }

}
