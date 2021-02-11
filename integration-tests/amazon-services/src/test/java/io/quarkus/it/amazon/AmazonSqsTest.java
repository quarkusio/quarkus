package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSqsTest {

    private final static String QUEUE_NAME = "quakrus-sqs-queue";
    private final static List<String> MESSAGES = new ArrayList<>();
    static {
        MESSAGES.add("Quarkus");
        MESSAGES.add("is");
        MESSAGES.add("awsome");
    }

    @BeforeEach
    public void before() {
        RestAssured.given().pathParam("queueName", QUEUE_NAME).post("/test/sqs/queue/{queueName}");
    }

    @AfterEach
    public void after() {
        RestAssured.given().pathParam("queueName", QUEUE_NAME).delete("/test/sqs/queue/{queueName}");
    }

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    public void testSendAndReceiveMessage(String endpoint) {
        //Send messages
        MESSAGES.forEach(msg -> {
            RestAssured.given()
                    .pathParam("endpoint", endpoint)
                    .pathParam("queueName", QUEUE_NAME)
                    .queryParam("msg", msg)
                    .when().post("/test/sqs/{endpoint}/{queueName}")
                    .then().body(any(String.class));
        });

        //Receive messages
        RestAssured.given()
                .pathParam("endpoint", endpoint)
                .pathParam("queueName", QUEUE_NAME)
                .when().get("/test/sqs/{endpoint}/{queueName}")
                .then()
                .body(is("Quarkus is awsome"));
    }
}
