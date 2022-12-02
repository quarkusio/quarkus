package io.quarkus.smallrye.metrics.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.smallrye.metrics.MetricRegistries;

/**
 * Verifies that during reload, vendor and base metric registries get cleaned up.
 */
public class DevModeMetricRegistryTest {

    @Path("/")
    public static class MetricsResource {

        @POST
        @Path("/create/{scope}/{name}")
        public void createMetric(@PathParam("scope") String scope, @PathParam("name") String name) {
            MetricRegistries.get(MetricRegistry.Type.valueOf(scope.toUpperCase())).counter(name);
        }

        @GET
        @Path("/check/{scope}/{name}")
        @Produces("text/plain")
        public Boolean doesMetricExist(@PathParam("scope") String scope, @PathParam("name") String name) {
            return MetricRegistries.get(MetricRegistry.Type.valueOf(scope.toUpperCase()))
                    .getCounters().containsKey(new MetricID(name));
        }

    }

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DevModeMetricRegistryTest.class, MetricsResource.class));

    @Test
    public void verifyRegistryCleanup() {
        when().post("/create/base/foo").then().statusCode(204);
        when().post("/create/vendor/foo").then().statusCode(204);

        // just to trigger a reload, we don't really need to change anything
        TEST.modifySourceFile(DevModeMetricRegistryTest.class, (s) -> s + " ");

        when().get("/check/base/foo").then().body(equalTo("false"));
        when().get("/check/vendor/foo").then().body(equalTo("false"));
    }

}
