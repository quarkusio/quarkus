package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.test.QuarkusUnitTest;

public class HttpWithLimitedUriTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.max-uri-tags", "1")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(EchoResource.class));

    @Inject
    HttpServerConfig httpServerConfig;

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        Assertions.assertEquals(1, httpServerConfig.maxUriTags);

        // Now let's poke the bear

        // Rest requests are nicely parameterized (should remain @ 1)
        when().get("/echo/ping").then().statusCode(200);
        when().get("/echo/pong").then().statusCode(200);
        when().get("/echo/other").then().statusCode(200);
        Assertions.assertEquals(1, registry.find("http.server.requests").timers().size());

        // we set a limit of 1. If we now request the other URL.. it should not appear in metrics
        when().get("/other").then().statusCode(200);
        Assertions.assertEquals(1, registry.find("http.server.requests").timers().size());
    }

    @Path("/")
    public static class EchoResource {
        @GET
        @Path("/echo/{message}")
        public String echo(@PathParam("message") String message) {
            return message;
        }

        @GET
        @Path("/other")
        public String other() {
            return "boo";
        }
    }

}
