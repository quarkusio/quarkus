package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.test.QuarkusUnitTest;

public class UriWithMaxTagMeterFilterTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.max-uri-tags", "1")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class));

    @Inject
    HttpServerConfig httpServerConfig;

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        Assertions.assertEquals(1, httpServerConfig.maxUriTags);

        // Server will have both /ping/{message} and /pong/{message}
        // Due to limit, there should only be one.
        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);
        Assertions.assertEquals(1, registry.find("http.server.requests").timers().size());
    }

}
