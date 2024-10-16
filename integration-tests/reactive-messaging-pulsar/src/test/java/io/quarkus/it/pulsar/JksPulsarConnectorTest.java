package io.quarkus.it.pulsar;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTestResource(value = PulsarResource.class, initArgs = {
        @ResourceArg(name = "isJks", value = "true"),
        @ResourceArg(name = "keyStorePassword", value = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L"),
        @ResourceArg(name = "trustStorePassword", value = "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L"),
        @ResourceArg(name = "pulsar.tls-configuration-name", value = "custom-jks"),
}, restrictToAnnotatedClass = true)
@QuarkusTest
public class JksPulsarConnectorTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    public void testFruits() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/pulsar/fruits").as(TYPE_REF).size(), 4));
    }

}
