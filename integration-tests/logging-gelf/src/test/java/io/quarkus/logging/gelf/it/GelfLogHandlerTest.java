package io.quarkus.logging.gelf.it;

import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * This test is disabled by default as it needs a central log management system up and running to be able to be launched.
 * Check the README.md, it contains info of how to launch one prior to this test.
 *
 * This test is designed to be launched with Graylog as the central management solution as the RestAssured assertion
 * check that a log events is received using the Graylog search API. Launching the test with another solution will
 * fail the test.
 */
@QuarkusTest
public class GelfLogHandlerTest {

    @Test
    public void test() throws InterruptedException {
        RestAssured.given().when().get("/gelf-log-handler").then().statusCode(204);

        //wait two seconds for the log events to be processed
        Thread.sleep(2000);

        RestAssured.given()
                .when()
                .auth().basic("admin", "admin")
                .get("http://127.0.0.1:9000/api/search/universal/relative?query=")
                .then().statusCode(200)
                .body("messages.message.message", hasItem("Some useful log message"));
    }
}
