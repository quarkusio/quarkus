package io.quarkus.it.amqp;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(AmqpBroker.class)
public class AmqpConnectorTest {

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void test() {
        await().until(() -> get("/amqp/people").as(TYPE_REF).size() >= 3);
    }

}
