package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.restassured.RestAssured;
import io.vertx.core.http.impl.HttpServerRequestInternal;

class UriTagWithInterfaceResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar.addClasses(Util.class, InterfaceResourceReturningUrlTemplate.class,
                    InterfaceResourceReturningUrlTemplateImpl.class));

    @Inject
    MeterRegistry registry;

    @Test
    void testRequestUris() throws Exception {
        RestAssured.basePath = "/";

        when().get("/class/path/method/path/test").then().statusCode(200)
                .body(Matchers.is("/class/path/method/path/{param}"));

        Util.waitForMeters(registry.find("http.server.requests").timers(), 1);

        System.out.println("Server paths\n" + Util.listMeters(registry, "http.server.requests"));

        Assertions.assertEquals(1,
                registry.find("http.server.requests").tag("uri", "/class/path/method/path/{param}").timers().size(),
                Util.foundServerRequests(registry, "Servlet path (/servlet) should be returned by JAX-RS."));
    }

    @Path("/class/path")
    public interface InterfaceResourceReturningUrlTemplate {

        @Path("method/path/{param}")
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String getTemplatizedPath(@PathParam("param") String param);

    }

    public static class InterfaceResourceReturningUrlTemplateImpl implements InterfaceResourceReturningUrlTemplate {

        private final CurrentVertxRequest request;

        public InterfaceResourceReturningUrlTemplateImpl(CurrentVertxRequest request) {
            this.request = request;
        }

        @Override
        public String getTemplatizedPath(String param) {
            return ((HttpServerRequestInternal) request.getCurrent().request()).context().getLocal("UrlPathTemplate");
        }
    }

}
