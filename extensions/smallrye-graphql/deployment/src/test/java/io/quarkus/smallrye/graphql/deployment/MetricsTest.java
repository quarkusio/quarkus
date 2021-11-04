package io.quarkus.smallrye.graphql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.metrics.MetricRegistries;

public class MetricsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.metrics.enabled=true"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    // Run a Query and check that its corresponding metric is updated
    @Test
    public void testQuery() {
        MetricRegistry metricRegistry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        SimpleTimer metric = metricRegistry.getSimpleTimers()
                .get(new MetricID("mp_graphql", new Tag("type", "QUERY"), new Tag("name", "ping"), new Tag("source", "false")));
        assertNotNull(metric, "Metrics should be registered eagerly");

        String pingRequest = getPayload("{\n" +
                "  ping {\n" +
                "    message\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept("application/json")
                .contentType("application/json")
                .body(pingRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"ping\":{\"message\":\"pong\"}}}"));

        assertEquals(1L, metric.getCount(), "Metric should be updated after querying");
    }

    private String getPayload(String query) {
        JsonObject jsonObject = createRequestBody(query);
        return jsonObject.toString();
    }

    private JsonObject createRequestBody(String graphQL) {
        return createRequestBody(graphQL, null);
    }

    private JsonObject createRequestBody(String graphQL, JsonObject variables) {
        // Create the request
        if (variables == null || variables.isEmpty()) {
            variables = Json.createObjectBuilder().build();
        }
        return Json.createObjectBuilder().add("query", graphQL).add("variables", variables).build();
    }

}
