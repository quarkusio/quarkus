package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ProvidedHealthChecksTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.smallrye-health.provided-checks.heap-memory.enabled", "true")
            .overrideConfigKey("quarkus.smallrye-health.provided-checks.system-load.enabled", "true")
            .overrideConfigKey("quarkus.smallrye-health.provided-checks.system-load.max", "0.8")
            .overrideConfigKey("quarkus.smallrye-health.provided-checks.thread.enabled", "true")
            .overrideConfigKey("quarkus.smallrye-health.provided-checks.thread.max", "123456");

    @Test
    public void testHeapMemoryHealthCheck() {
        when().get("/health/live").then()
                .body("checks.findAll{ it.name == 'heap-memory'}", hasSize(1));
    }

    @Test
    public void testSystemLoadHealthCheck() {
        when().get("/health/live").then()
                .body("checks.findAll{ it.name == 'system-load'}", hasSize(1))
                .body("checks.findAll{ it.name == 'system-load'}[0].data.'loadAverage max'", equalTo("0.8"));
    }

    @Test
    public void testThreadHealthCheck() {
        when().get("/health/live").then()
                .body("checks.findAll{ it.name == 'threads'}", hasSize(1))
                .body("checks.findAll{ it.name == 'threads'}[0].data.'max thread count'", equalTo(123456));
    }

}
