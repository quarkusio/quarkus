package io.quarkus.it.neo4j;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;

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

    @Inject Driver driver;

    // This creates nodes as well and uses an unmanaged tx. It is therefore also testing that the
    // JTA aware driver behaves just like the normal one when not managed
    @Test
    public void testBlockingNeo4jFunctionality() {
        RestAssured.given().when().get("/neo4j/blocking").then().body(is("OK"));
    }

    @Test
    public void testTransactionalNeo4jFunctionalityCommiting() {
        var externalId = UUID.randomUUID().toString();
        RestAssured.given().when()
            .queryParam("externalId", externalId)
            .get("/neo4j/transactional").then().body(is("OK"));

        assertEquals(1L, numberOfFrameworkNodesWithId(externalId));
    }

    @Test
    public void testTransactionalNeo4jFunctionalityRollback() {
        var externalId = UUID.randomUUID().toString();
        RestAssured.given().when()
            .queryParam("externalId", externalId)
            .queryParam("causeAScene", true)
            .get("/neo4j/transactional")
            .then().log().all()
            .statusCode(500)
            .body(is(equalTo("On purpose.")));

        assertEquals(0L, numberOfFrameworkNodesWithId(externalId));
    }

    @Test
    public void testItShouldNotBeAllowedToUseJTAAndLocalTX() {
        RestAssured.given().when()
            .get("/neo4j/not-allowed-to-use-jta-and-local-tx")
            .then().log().all()
            .statusCode(500)
            .body(is(equalTo("Unmanaged transactions are not supported in a managed (JTA) environment.")));
    }

    private long numberOfFrameworkNodesWithId(String externalId) {
        try (var session = driver.session()) {
            return session.run("MATCH (n:Framework {id: $id}) RETURN count(n)", Values.parameters("id", externalId))
                .single().get(0).asLong();
        }
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
        RestAssured.when().get("/q/health/ready").then()
                .log().all()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Neo4j connection health check"),
                        "checks.data.server", containsInAnyOrder(matchesRegex("Neo4j/.*@.*:\\d*")),
                        "checks.data.edition", containsInAnyOrder(is(notNullValue())));
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
                .when().get("/q/metrics/vendor/" + name)
                .then()
                .body("'" + name + "'", valueMatcher);
    }
}
