package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.util.EventDataProvider.getData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.test.model.BatchItemFailures;
import io.quarkus.funqy.test.model.ItemFailure;
import io.quarkus.funqy.test.util.EventDataProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DynamoDbFunctionTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("item-function.properties", "application.properties")
                    .addAsResource("events/dynamodb", "events")
                    .addClasses(TestFunctions.class, Item.class,
                            BatchItemFailures.class, ItemFailure.class,
                            EventDataProvider.class));

    @Test
    public void should_fail_on_dynamodb_event_without_dynamodb_event_type() {
        // given
        var body = getData("ok.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        // It is not supported to transform the DynamoDB event record to an internal model. Therefore, if somebody
        // would try this, the lambda would return every message as failure in batch item failures and log an error.
        var respBody = response.then().statusCode(200)
                .extract().body().as(BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), hasSize(2));
        assertThat(respBody.batchItemFailures().stream().map(ItemFailure::itemIdentifier).toList(), hasItems("1", "2"));
    }

    @Test
    public void should_fail_on_dynamodb_event_via_pipes_without_dynamodb_event_type() {
        // given
        var body = getData("pipes-ok.json");

        // when
        var response = RestAssured.given().contentType("application/json")
                .body(body)
                .post("/");

        // then
        // It is not supported to transform the DynamoDB event record to an internal model. Therefore, if somebody
        // would try this, the lambda would return every message as failure in batch item failures and log an error.
        var respBody = response.then().statusCode(200)
                .extract().body().as(BatchItemFailures.class);
        assertThat(respBody.batchItemFailures(), hasSize(2));
        assertThat(respBody.batchItemFailures().stream().map(ItemFailure::itemIdentifier).toList(), hasItems("1", "2"));
    }
}
