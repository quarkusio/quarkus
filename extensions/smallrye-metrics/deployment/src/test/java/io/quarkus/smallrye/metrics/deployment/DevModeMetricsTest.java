package io.quarkus.smallrye.metrics.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DevModeMetricsTest.class, MetricsResource.class));

    @Test
    public void test() {
        // increment the mycounter twice and verify its value
        when().get("/increment-counter").then().statusCode(204);
        when().get("/increment-counter").then().statusCode(204);
        when().get("/getvalue/mycounter").then().body(equalTo("2"));

        // trigger a reload by adding a new metric (mycounter2)
        TEST.modifySourceFile(DevModeMetricsTest.class, (s) -> s.replaceFirst("MARKER-KEEP-ME",
                "MARKER-KEEP-ME\n" +
                        "@Counted(name = \"mycounter2\", absolute = true)\n" +
                        "@GET\n" +
                        "@Path(\"/increment-counter2\")\n" +
                        "public void countedMethod2() {\n" +
                        "} //MARKER-END"));

        // check that the original mycounter is re-registered and its value is zero
        when().get("/getvalue/mycounter").then().body(equalTo("0"));

        // check that mycounter2 is present and can be used
        when().get("/getvalue/mycounter2").then().body(equalTo("0"));
        when().get("/increment-counter2").then().statusCode(204);
        when().get("/getvalue/mycounter2").then().body(equalTo("1"));

        // trigger a reload by removing mycounter2 again
        TEST.modifySourceFile(DevModeMetricsTest.class,
                (s) -> s.replaceFirst("//MARKER-KEEP-ME[\\s\\S]+?MARKER-END", ""));

        // verify that mycounter2 no longer exists
        when().get("/getvalue/mycounter2").then()
                .statusCode(204);
    }

}
