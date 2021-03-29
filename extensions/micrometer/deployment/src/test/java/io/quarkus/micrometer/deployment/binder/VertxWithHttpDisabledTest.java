package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import java.net.URL;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;

public class VertxWithHttpDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PingPongResource.class, PingPongResource.PingPongRestClient.class));

    @TestHTTPResource
    URL url;

    @Inject
    Instance<VertxMeterBinderAdapter> vertxMeterBinderAdapterInstance;

    @Inject
    HttpBinderConfiguration httpBinderConfiguration;

    @Inject
    MeterRegistry registry;

    @Test
    public void testVertxMetricsWithoutHttp() throws Exception {
        Assertions.assertFalse(httpBinderConfiguration.isClientEnabled());
        Assertions.assertFalse(httpBinderConfiguration.isServerEnabled());

        // Vertx Binder should exist
        Assertions.assertTrue(vertxMeterBinderAdapterInstance.isResolvable());
        VertxMeterBinderAdapter adapter = vertxMeterBinderAdapterInstance.get();

        // HttpServerMetrics should not be created (null returned) because
        // Http server metrics are disabled
        Assertions.assertNull(adapter.createHttpServerMetrics(new HttpServerOptions(), new SocketAddress() {
            @Override
            public String host() {
                return "a.b.c";
            }

            @Override
            public int port() {
                return 0;
            }

            @Override
            public String path() {
                return null;
            }
        }));

        // If you invoke requests, no http server or client meters should be registered

        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);

        Assertions.assertEquals(0, registry.find("http.server.requests").timers().size());
        Assertions.assertEquals(0, registry.find("http.client.requests").timers().size());
    }
}
