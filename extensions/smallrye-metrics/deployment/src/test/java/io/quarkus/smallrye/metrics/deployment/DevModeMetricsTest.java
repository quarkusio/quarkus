package io.quarkus.smallrye.metrics.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.File;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class DevModeMetricsTest {

    @Path("/")
    public static class MetricsResource {

        @Inject
        MetricRegistry metricRegistry;

        @Counted(name = "mycounter", absolute = true)
        @GET
        @Path("/increment-counter")
        public void countedMethod() {
        }

        //MARKER-KEEP-ME

        @GET
        @Path("/getvalue/{name}")
        @Produces("text/plain")
        public Long getCounterValue(@PathParam("name") String name) {
            Counter counter = metricRegistry.getCounters().get(new MetricID(name));
            if (counter != null) {
                return counter.getCount();
            } else {
                return null;
            }
        }

    }

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(
                            new File("src/test/resources/config/jaxrs-metrics.properties"),
                            "application.properties")
                    .addClasses(DevModeMetricsTest.class, MetricsResource.class));

    @Test
    public void test() {
        // increment the mycounter twice and verify its value
        when().get("/increment-counter").then().statusCode(204);
        when().get("/increment-counter").then().statusCode(204);
        when().get("/getvalue/mycounter").then().body(equalTo("2"));

        // jax-rs metrics are disabled
        when().get("/q/metrics").then()
                .body(not(containsString("io.quarkus.smallrye.metrics.deployment.DevModeMetricsTest$MetricsResource")));

        // trigger a reload by adding a new metric (mycounter2)
        TEST.modifySourceFile(DevModeMetricsTest.class, (s) -> s.replaceFirst("MARKER-KEEP-ME",
                "MARKER-KEEP-ME\n" +
                        "@Counted(name = \"mycounter2\", absolute = true)\n" +
                        "@GET\n" +
                        "@Path(\"/increment-counter2\")\n" +
                        "public void countedMethod2() {\n" +
                        "} //MARKER-END"));

        // enable jax-rs metrics via quarkus.resteasy.metrics.enabled
        TEST.modifyResourceFile("application.properties",
                v -> v.replace("quarkus.resteasy.metrics.enabled=false", "quarkus.resteasy.metrics.enabled=true"));

        // check that the original mycounter is re-registered and its value is zero
        when().get("/getvalue/mycounter").then().body(equalTo("0"));

        // check that mycounter2 is present and can be used
        when().get("/getvalue/mycounter2").then().body(equalTo("0"));
        when().get("/increment-counter2").then().statusCode(204);
        when().get("/getvalue/mycounter2").then().body(equalTo("1"));

        // jax-rs metrics are enabled
        when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("io.quarkus.smallrye.metrics.deployment.DevModeMetricsTest$MetricsResource"));

        // disable jax-rs metrics via quarkus.resteasy.metrics.enabled
        TEST.modifyResourceFile("application.properties",
                v -> v.replace("quarkus.resteasy.metrics.enabled=true", "quarkus.resteasy.metrics.enabled=false"));

        // trigger a reload by removing mycounter2 again
        TEST.modifySourceFile(DevModeMetricsTest.class,
                (s) -> s.replaceFirst("//MARKER-KEEP-ME[\\s\\S]+?MARKER-END", ""));

        // verify that mycounter2 no longer exists
        when().get("/getvalue/mycounter2").then()
                .statusCode(204);

        // jax-rs metrics are disabled
        when().get("/q/metrics").then()
                .body(not(containsString("io.quarkus.smallrye.metrics.deployment.DevModeMetricsTest$MetricsResource")));

        // enable jax-rs metrics via quarkus.resteasy.metrics.enabled
        TEST.modifyResourceFile("application.properties",
                v -> v.replace("quarkus.smallrye-metrics.jaxrs.enabled=false", "quarkus.smallrye-metrics.jaxrs.enabled=true"));

        // verify that mycounter2 no longer exists
        when().get("/getvalue/mycounter2").then()
                .statusCode(204);

        // jax-rs metrics are enabled
        when().get("/q/metrics").then()
                .body(containsString("io.quarkus.smallrye.metrics.deployment.DevModeMetricsTest$MetricsResource"));

    }

}
