package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KafkaConnectorTest {
    private static final Logger log = Logger.getLogger(KafkaTestResource.class);

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void test() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/people").as(TYPE_REF).size(), 6));
    }

}
