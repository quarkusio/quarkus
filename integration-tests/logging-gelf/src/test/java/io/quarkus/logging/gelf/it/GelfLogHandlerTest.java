package io.quarkus.logging.gelf.it;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void init() throws IOException, InterruptedException {
        HttpPost request = new HttpPost("http://localhost:9000/api/system/inputs");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        request.setHeader("X-Requested-By", "Apache");
        HttpEntity entity = new StringEntity(
                "{\"title\":\"udp input\",\"configuration\":{\"recv_buffer_size\":262144,\"bind_address\":\"0.0.0.0\",\"port\":12201,\"decompress_size_limit\":8388608},\"type\":\"org.graylog2.inputs.gelf.udp.GELFUDPInput\",\"global\":true}",
                ContentType.APPLICATION_JSON);
        request.setEntity(entity);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new RuntimeException("Unable to create the Graylog UDP input: " + response.getStatusLine());
        }

        // wait for the input to be running
        await().during(10, TimeUnit.SECONDS);
    }

    @Test
    public void test() {
        RestAssured.given().when().get("/gelf-log-handler").then().statusCode(204);

        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            RestAssured.given()
                                    .when()
                                    .auth().basic("admin", "admin")
                                    .get("http://127.0.0.1:9000/api/search/universal/relative?query=message")
                                    .then().statusCode(200)
                                    .body("messages.message.message", hasItem("Some useful log message"));
                        });
    }
}
