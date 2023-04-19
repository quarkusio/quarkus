package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import java.time.Duration

import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.await
import static org.hamcrest.CoreMatchers.is

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
class ReactiveMessagingTest {

    @Test
    void test() {
        assertCountries(6)

        given()
            .when()
            .post("/country/kafka/dummy")
            .then()
            .statusCode(200)

        assertCountries(8)
    }

    private static def assertCountries(int num) {
        await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(5)).untilAsserted {
            given()
                .when()
                .get("/country/resolved")
                .then()
                .body("size()", is(num))
        }
    }
}
