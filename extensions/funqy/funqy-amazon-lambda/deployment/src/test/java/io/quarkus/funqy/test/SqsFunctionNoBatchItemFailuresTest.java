package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.util.EventDataProvider.getData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.test.util.EventDataProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SqsFunctionNoBatchItemFailuresTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("no-batch-function.properties", "application.properties")
                    .addAsResource("events/sqs", "events")
                    .addClasses(TestFunctions.class, Item.class,
                            EventDataProvider.class));

    @Test
    public void should_return_no_failures_if_processing_is_ok() {
        // given
        var body = getData("ok.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        response.then().statusCode(204);
    }

    @Test
    public void should_return_one_failure_if_processing_fails() {
        // given
        var body = getData("fail.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        response.then().statusCode(204);
    }
}
