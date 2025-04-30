package io.quarkus.it.pulsar;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTestResource(value = PulsarResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class PulsarConnectorTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    public void testFruits() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/pulsar/fruits").as(TYPE_REF).size(), 4));
    }

}
