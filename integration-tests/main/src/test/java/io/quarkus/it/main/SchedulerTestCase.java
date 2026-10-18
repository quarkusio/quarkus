package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
public class SchedulerTestCase {

    @Test
    public void testCount() throws InterruptedException {
        // Wait at least 1 second
        Thread.sleep(1000);
        Response response = given()
                .when().get("/scheduler/count");
        String body = response.asString();
        int count = Integer.valueOf(body.split(":")[1]);
        assertTrue(count > 0);
        response
                .then()
                .statusCode(200);
    }

}
