package io.quarkus.it.artemis;

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
public class ArtemisConsumerTest implements ArtemisHelper {

    @Test
    public void test() throws Exception {
        String body = createBody();
        try (Session session = createSession()) {
            session.createProducer(session.createQueue("test-jms")).send(session.createTextMessage(body));
        }

        Response response = RestAssured.with().body(body).get("/artemis");
        Assertions.assertEquals(StatusCodes.OK, response.statusCode());
        Assertions.assertEquals(body, response.getBody().asString());
    }
}
