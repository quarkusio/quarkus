package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSnsTest {

    private final static String TOPIC_NAME = "quakrus-sns";

    @BeforeEach
    public void before() {
        RestAssured.given().pathParam("topicName", TOPIC_NAME).post("/test/sns/topics/{topicName}");
    }

    @AfterEach
    public void after() {
        RestAssured.given().pathParam("topicName", TOPIC_NAME).delete("/test/sns/topics/{topicName}");
    }

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    public void testPublishAndReceive(String endpoint) {
        String message = "Quarkus is awsome";
        //Publish message
        RestAssured.given()
                .pathParam("endpoint", endpoint)
                .pathParam("topicName", TOPIC_NAME)
                .queryParam("msg", message)
                .when().post("/test/sns/{endpoint}/publish/{topicName}")
                .then().body(any(String.class));

        //Receive messages
        RestAssured.given()
                .pathParam("topicName", TOPIC_NAME)
                .when().get("/test/sns/topics/{topicName}")
                .then()
                .body(containsString("Quarkus is awsome"));
    }
}
