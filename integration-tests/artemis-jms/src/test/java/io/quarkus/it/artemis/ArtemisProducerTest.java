package io.quarkus.it.artemis;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

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

        try (Session session = createSession()) {
            MessageConsumer consumer = session.createConsumer(session.createQueue("test-jms"));
            Message message = consumer.receive(1000L);
            Assertions.assertEquals(body, message.getBody(String.class));
        }
    }
}
