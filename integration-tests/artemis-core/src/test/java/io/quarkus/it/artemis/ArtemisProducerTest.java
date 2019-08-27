package io.quarkus.it.artemis;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.undertow.httpcore.StatusCodes;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisProducerTest implements ArtemisHelper {

    @Test
    public void test() throws Exception {
        String body = createBody();
        Response response = RestAssured.with().body(body).post("/artemis");
        Assertions.assertEquals(StatusCodes.NO_CONTENT, response.statusCode());

        try (ClientSession session = createSession()) {
            session.start();
            ClientMessage message = session.createConsumer("test-core").receive(1000L);
            message.acknowledge();
            Assertions.assertEquals(body, message.getBodyBuffer().readString());
        }
    }
}
