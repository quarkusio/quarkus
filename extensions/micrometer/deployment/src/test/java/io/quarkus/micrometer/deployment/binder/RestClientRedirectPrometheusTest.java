package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusExtensionTest;

public class RestClientRedirectPrometheusTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.rest-client.redirect-client.url", "${test.url}")
            .overrideConfigKey("quarkus.rest-client.redirect-client.follow-redirects", "true")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Util.class, RedirectResource.class, RedirectResource.RedirectClient.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void redirectShouldNotRegisterConflictingClientMeters() throws InterruptedException {
        when()
                .get("/exercise")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World!"));

        Util.waitForMeters(registry.find("http.client.requests").timers(), 1);
        Assertions.assertEquals(1, registry.find("http.client.requests").timers().size(),
                Util.foundClientRequests(registry, "Expected a single REST client timer after following a redirect."));
    }

    @Path("/")
    @Singleton
    public static class RedirectResource {

        @RegisterRestClient(configKey = "redirect-client")
        public interface RedirectClient {

            @GET
            @Path("/redirect")
            @Produces(MediaType.TEXT_PLAIN)
            Response getThroughRedirect();
        }

        @Inject
        @RestClient
        RedirectClient redirectClient;

        @GET
        @Path("/redirect")
        public Response redirect() {
            return Response.status(302)
                    .header("Location", "/target")
                    .build();
        }

        @GET
        @Path("/target")
        @Produces(MediaType.TEXT_PLAIN)
        public String target() {
            return "Hello World!";
        }

        @GET
        @Path("/exercise")
        @Produces(MediaType.TEXT_PLAIN)
        public String exercise() {
            try (Response response = redirectClient.getThroughRedirect()) {
                return response.readEntity(String.class);
            }
        }
    }
}
