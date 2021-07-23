package io.quarkus.it.kafka;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class KafkaAdminTest {

    @Test
    public void test() {
        RestAssured.when().get("/kafka/partitions/test").then().body(Matchers.is("2"));
        RestAssured.when().get("/kafka/partitions/test-consumer").then().body(Matchers.is("3"));
    }

    @Test
    public void health() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Kafka connection health check"));
    }

}
