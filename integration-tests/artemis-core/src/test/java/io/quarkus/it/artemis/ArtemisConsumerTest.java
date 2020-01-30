package io.quarkus.it.artemis;

import javax.ws.rs.core.Response.Status;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class ArtemisConsumerTest implements ArtemisHelper {

    @Test
    public void test() throws Exception {
        String body = createBody();
        try (ClientSession session = createSession()) {
            ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString(body);
            session.createProducer("test-core").send(message);
        }

        Response response = RestAssured.with().body(body).get("/artemis");
        Assertions.assertEquals(Status.OK.getStatusCode(), response.statusCode());
        Assertions.assertEquals(body, response.getBody().asString());
    }
}
