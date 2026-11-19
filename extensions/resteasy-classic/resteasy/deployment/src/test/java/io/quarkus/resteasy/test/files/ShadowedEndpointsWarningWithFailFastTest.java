package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithFailFastTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, MultiResource.class)
                    .addAsResource(new StringAsset("static hello"), "META-INF/resources/api/hello")
                    .addAsResource(new StringAsset("multi"), "META-INF/resources/api/multi"))
            // RESTEasy fails the requests matching several endpoints equally, e.g. the requests to /api/multi without
            // the Accept header, the check skips them instead of failing the startup
            .overrideRuntimeConfigKey("resteasy.fail.fast.on.multiple.resources.matching", "true")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD /api/hello -> " + HelloResource.class.getName() + "#hello"));

    @Test
    void staticResourcesAreServedInsteadOfTheShadowedEndpoints() {
        when().get("/api/hello").then().statusCode(200).body(is("static hello"));
        when().get("/api/multi").then().statusCode(200).body(is("multi"));
    }

    @Path("/api/hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello endpoint";
        }
    }

    @Path("/api/multi")
    public static class MultiResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String text() {
            return "text";
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String json() {
            return "{}";
        }
    }
}
