package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.util.EventDataProvider.getData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.test.model.BatchItemFailures;
import io.quarkus.funqy.test.model.ItemFailure;
import io.quarkus.funqy.test.util.BodyDeserializer;
import io.quarkus.funqy.test.util.EventDataProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Testing that the item-function with a customer model can handle kinesis events.
 */
public class KinesisFunctionTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("item-function.properties", "application.properties")
                    .addAsResource("events/kinesis", "events")
                    .addClasses(TestFunctions.class, Item.class,
                            BatchItemFailures.class, ItemFailure.class,
                            EventDataProvider.class, BodyDeserializer.class));

    @Inject
    BodyDeserializer deserializer;

    @Test
    public void should_return_no_failures_if_processing_is_ok() {
        // given
        var body = getData("ok.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        var respBody = deserializer.getBodyAs(response.then().statusCode(200), BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), is(empty()));
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
        var respBody = deserializer.getBodyAs(response.then().statusCode(200), BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), hasSize(1));
        assertThat(respBody.batchItemFailures().stream().map(ItemFailure::itemIdentifier).toList(), hasItem("1"));
    }

    @Test
    public void should_return_no_failures_if_processing_pipes_is_ok() {
        // given
        var body = getData("pipes-ok.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        var respBody = deserializer.getBodyAs(response.then().statusCode(200), BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), is(empty()));
    }

    @Test
    public void should_return_one_failure_if_processing_pipes_fails() {
        // given
        var body = getData("pipes-fail.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        var respBody = deserializer.getBodyAs(response.then().statusCode(200), BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), hasSize(1));
        assertThat(respBody.batchItemFailures().stream().map(ItemFailure::itemIdentifier).toList(), hasItem("1"));
    }
}
