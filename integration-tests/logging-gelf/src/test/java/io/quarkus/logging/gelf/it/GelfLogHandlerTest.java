package io.quarkus.logging.gelf.it;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * This test needs a central log management system up and running to be able to be launched.
 * Check the README.md, it contains info of how to launch one prior to this test.
 * In the CI, containers are launched via the docker-maven-plugin.
 *
 * This test is designed to be launched with ELK as the central management solution as the RestAssured assertion
 * check that a log events is received using the Elasticsearch search API. Launching the test with another solution will
 * fail the test.
 */
@QuarkusTest
public class GelfLogHandlerTest {

    @Test
    public void test() {
        //we need to await for a certain time as logstash needs to create the index template,
        // then elasticsearch create the index
        // then some logs being indexed.
        await().atMost(40, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            RestAssured.given().when().get("/gelf-log-handler").then().statusCode(204);

                            Response response = RestAssured.given()
                                    .when()
                                    .get("http://127.0.0.1:9200/_search?q=message")
                                    .andReturn();

                            assertEquals(200, response.statusCode());
                            assertNotNull(response.body().path("hits.hits[0]._source"));
                            assertEquals("Some useful log message", response.body().path("hits.hits[0]._source.message"));
                            assertEquals(Integer.valueOf(98), response.body().path("hits.hits[0]._source.field4"));
                            assertEquals("99", response.body().path("hits.hits[0]._source.field3"));
                        });
    }
}
