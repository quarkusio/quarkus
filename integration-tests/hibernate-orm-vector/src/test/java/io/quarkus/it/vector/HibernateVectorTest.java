package io.quarkus.it.vector;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HibernateVectorTest {

    @Test
    public void testVectorRoundTrip() {
        // Verify that a persisted float[] embedding is retrieved correctly
        RestAssured.when().get("/hibernate-vector-test/vector/1").then()
                .body(containsString("1.0"));
    }

    @Test
    public void testVectorRoundTripSecond() {
        RestAssured.when().get("/hibernate-vector-test/vector/2").then()
                .body(containsString("4.0"));
    }

    @Test
    public void testPreciseVectorRoundTrip() {
        RestAssured.when().get("/hibernate-vector-test/vector/precise/1").then()
                .body(containsString("1.0"));
    }

    @Test
    public void testNearestNeighbor() {
        RestAssured.when().get("/hibernate-vector-test/vector/nearest/1").then()
                .body(containsString("2"));
    }

    @Test
    public void testNearestNeighborL2() {
        RestAssured.when().get("/hibernate-vector-test/vector/nearest-l2/1").then()
                .body(containsString("2"));
    }
}
