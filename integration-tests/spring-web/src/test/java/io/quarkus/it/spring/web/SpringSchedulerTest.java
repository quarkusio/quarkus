package io.quarkus.it.spring.web;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpringSchedulerTest {

    @Test
    public void testCount() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String body = given()
                    .when().get("/spring/scheduler/count")
                    .then().statusCode(200)
                    .extract().asString();
            int count = Integer.parseInt(body.split(":")[1]);
            assertThat("Scheduled invocation count", count, greaterThan(0));
        });
    }

}
