package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.test.HelloResource;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.test.QuarkusUnitTest;

public class UriWithMaxTagMeterFilterTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.max-uri-tags", "1")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.max-uri-tags", "1")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, PingPongResource.class, PingPongResource.PingPongRestClient.class));

    @Inject
    HttpServerConfig httpServerConfig;
    @Inject
    HttpClientConfig httpClientConfig;

    @Inject
    MeterRegistry registry;

    @Test
    public void test() throws Exception {
        Assertions.assertEquals(1, httpServerConfig.maxUriTags);
        Assertions.assertEquals(1, httpClientConfig.maxUriTags);

        // Server limit is constrained to 1
        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);
        when().get("/one").then().statusCode(200);
        Assertions.assertEquals(1, registry.find("http.server.requests").timers().size());

        // Client limit is constrained to 1
        Assertions.assertEquals(1, registry.find("http.client.requests").timers().size());
    }
}
