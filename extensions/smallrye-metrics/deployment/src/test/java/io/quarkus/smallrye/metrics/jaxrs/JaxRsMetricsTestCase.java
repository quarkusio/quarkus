package io.quarkus.smallrye.metrics.jaxrs;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.PathSegment;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JaxRsMetricsTestCase {

    final String METRIC_RESOURCE_CLASS_NAME = MetricsResource.class.getName();

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.smallrye-metrics.jaxrs.enabled=true"),
                            "application.properties")
                    .addClasses(MetricsResource.class));

    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    MetricRegistry metricRegistry;

    @Test
    public void testBasic() {
        when()
                .get("/hello/joe")
                .then()
                .statusCode(200);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "hello_java.lang.String"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

    @Test
    public void testMethodReturningServerError() throws InterruptedException {
        when()
                .get("/error")
                .then()
                .statusCode(501);
        // we only consider requests ending with a 500 as "unsuccessful",
        // so a 501 should count towards successful
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "error"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

    @Test
    public void testMethodThrowingException() {
        when()
                .get("/exception")
                .then()
                .statusCode(500);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "exception"));
        assertEquals(0, metric.getCount());
        assertEquals(0, metric.getElapsedTime().toNanos());

        // calls ending with status code 500 should only be reflected in the REST.request.unmappedException.total metric
        Counter exceptionCounter = metricRegistry.counter("REST.request.unmappedException.total",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "exception"));
        assertEquals(1, exceptionCounter.getCount());
    }

    @Test
    public void testMethodTakingList() {
        when()
                .get("/a/b/c/list")
                .then()
                .statusCode(200);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "list_java.util.List"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

    @Test
    public void testMethodTakingArray() {
        when()
                .get("/a/b/c/array")
                .then()
                .statusCode(200);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "array_" + PathSegment.class.getName() + "[]"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

    @Test
    public void testMethodTakingVarargs() {
        when()
                .get("/a/b/c/varargs")
                .then()
                .statusCode(200);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "varargs_" + PathSegment.class.getName() + "[]"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

    @Test
    public void testAsyncMethod() {
        when()
                .get("/async")
                .then()
                .statusCode(200);
        SimpleTimer metric = metricRegistry.simpleTimer("REST.request",
                new Tag("class", METRIC_RESOURCE_CLASS_NAME),
                new Tag("method", "async"));
        assertEquals(1, metric.getCount());
        assertTrue(metric.getElapsedTime().toNanos() > 0);
    }

}
