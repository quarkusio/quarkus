package io.quarkus.it.quartz;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class QuartzTestCase {

    @Test
    public void testCount() throws InterruptedException {
        // Wait at least 1 second
        Thread.sleep(1000);
        assertCounter("/scheduler/count");
        assertCounter("/scheduler/count/fix-8555");
    }

    @Test
    public void testDisabledMethodsShouldNeverBeExecuted() throws InterruptedException {
        // Wait at least 1 second
        Thread.sleep(1000);
        assertEmptyValueForDisabledMethod("/scheduler/disabled/cron");
        assertEmptyValueForDisabledMethod("/scheduler/disabled/every");
    }

    @Test
    public void testFixedInstanceIdGenerator() {
        assertExpectedBodyString("/scheduler/instance-id", "myInstanceId");
    }

    private void assertCounter(String counterPath) {
        Response response = given().when().get(counterPath);
        String body = response.asString();
        int count = Integer.valueOf(body);
        assertTrue(count > 0);
        response
                .then()
                .statusCode(200);
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

}
