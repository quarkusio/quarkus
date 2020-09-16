package io.quarkus.it.neo4j;

import static org.hamcrest.Matchers.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Test connecting via Neo4j Java-Driver to Neo4j.
 * Can quickly start a matching database with:
 *
 * <pre>
 *     docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/music' neo4j/neo4j-experimental:4.0.0-rc01
 * </pre>
 */
@QuarkusTest
public class Neo4jFunctionalityTest {

    @Test
    public void testBlockingNeo4jFunctionality() {
        RestAssured.given().when().get("/neo4j/blocking").then().body(is("OK"));
    }

    @Test
    public void testAsynchronousNeo4jFunctionality() {
        RestAssured.given()
                .when().get("/neo4j/asynchronous")
                .then().statusCode(200)
                .body(is(equalTo(Stream.of(1, 2, 3).map(i -> i.toString()).collect(Collectors.joining(",", "[", "]")))));
    }

    @Test
    public void testReactiveNeo4jFunctionality() {
        RestAssured.given()
                .when().get("/neo4j/reactive")
                .prettyPeek()
                .then().statusCode(200)
                .contentType("text/event-stream");
    }

    @Test
    public void health() {
        RestAssured.when().get("/health/ready").then()
                .log().all()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Neo4j connection health check"),
                        "checks.data.server", containsInAnyOrder(matchesRegex("Neo4j/.*@.*:\\d*")));
    }

    @Test
    public void metrics() {
        RestAssured.given().when().get("/neo4j/blocking").then().body(is("OK"));
        assertMetricValue("neo4j.acquired", greaterThan(0));
        assertMetricValue("neo4j.created", greaterThan(0));
        assertMetricValue("neo4j.totalAcquisitionTime", greaterThan(0));
        assertMetricValue("neo4j.totalConnectionTime", greaterThan(0));
        assertMetricValue("neo4j.totalInUseTime", greaterThan(0));
    }

    private void assertMetricValue(String name, Matcher<Integer> valueMatcher) {
        RestAssured
                .given().accept(ContentType.JSON)
                .when().get("/metrics/vendor/" + name)
                .then()
                .body("'" + name + "'", valueMatcher);
    }
}
