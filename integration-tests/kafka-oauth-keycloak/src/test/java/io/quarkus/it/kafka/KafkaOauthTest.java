package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(KafkaKeycloakTestResource.class)
public class KafkaOauthTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    public void test() {
        await().untilAsserted(() -> Assertions.assertEquals(2, get("/kafka").as(TYPE_REF).size()));
    }
}
