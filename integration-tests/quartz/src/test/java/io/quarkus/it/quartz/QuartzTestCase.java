package io.quarkus.it.quartz;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class QuartzTestCase {

    @Test
    public void testCount() {
        // ensure that scheduled job is called
        assertCounter("/scheduler/count", 1, Duration.ofSeconds(1));
        // assert programmatically scheduled job is called
        assertCounter("/scheduler/count/fix-8555", 2, Duration.ofSeconds(2));
    }

    @Test
    public void testDisabledMethodsShouldNeverBeExecuted() {
        // ensure that at least one scheduled job is called
        assertCounter("/scheduler/count", 1, Duration.ofSeconds(1));
        // then ensure that disabled jobs are never called
        assertEmptyValueForDisabledMethod("/scheduler/disabled/cron");
        assertEmptyValueForDisabledMethod("/scheduler/disabled/every");
    }

    @Test
    public void testFixedInstanceIdGenerator() {
        assertExpectedBodyString("/scheduler/instance-id", "myInstanceId");
    }

    @Test
    public void testProgrammaticJobs() {
        given().when().post("/scheduler/programmatic/register").then().statusCode(204);
        assertCounter("/scheduler/programmatic/sync", 1, Duration.ofSeconds(3));
        assertCounter("/scheduler/programmatic/async", 1, Duration.ofSeconds(3));
    }

    private void assertEmptyValueForDisabledMethod(String path) {
        assertExpectedBodyString(path, "");
    }

    private void assertExpectedBodyString(String path, String expectedBody) {
        Response response = given().when().get(path);
        String body = response.asString();
        assertEquals(expectedBody, body);
        response
                .then()
                .statusCode(200);
    }

    private void assertCounter(String counterPath, int expectedCount, Duration timeout) {
        await().atMost(timeout)
                .until(() -> {
                    Response response = given().when().get(counterPath);
                    int code = response.statusCode();
                    if (code != 200) {
                        return false;
                    }
                    String body = response.asString();
                    int count = Integer.valueOf(body);
                    return count >= expectedCount;
                });

    }

}
