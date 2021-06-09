package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.test.QuarkusUnitTest;

public class VertxWithHttpDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class));

    @Inject
    HttpBinderConfiguration httpBinderConfiguration;

    @Inject
    MeterRegistry registry;

    @Test
    public void testVertxMetricsWithoutHttp() throws Exception {
        Assertions.assertFalse(httpBinderConfiguration.isClientEnabled());
        Assertions.assertFalse(httpBinderConfiguration.isServerEnabled());

        // If you invoke requests, no http server or client meters should be registered
        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);

        Assertions.assertEquals(0, registry.find("http.server.requests").timers().size());
        Assertions.assertEquals(0, registry.find("http.client.requests").timers().size());
    }
}
