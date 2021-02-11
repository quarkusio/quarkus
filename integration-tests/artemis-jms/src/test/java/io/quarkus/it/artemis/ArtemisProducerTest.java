package io.quarkus.it.artemis;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisProducerTest implements ArtemisHelper {

    @Test
    public void test() throws Exception {
        String body = createBody();
        Response response = RestAssured.with().body(body).post("/artemis");
        Assertions.assertEquals(Status.NO_CONTENT.getStatusCode(), response.statusCode());

        try (JMSContext context = createContext()) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("test-jms"));
            Message message = consumer.receive(1000L);
            Assertions.assertEquals(body, message.getBody(String.class));
        }
    }
}
