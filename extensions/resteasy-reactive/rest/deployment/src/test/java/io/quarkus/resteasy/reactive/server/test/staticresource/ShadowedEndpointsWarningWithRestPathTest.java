package io.quarkus.resteasy.reactive.server.test.staticresource;

import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithRestPathTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class)
                    .addAsResource(new StringAsset("static secure"), "META-INF/resources/api/secure")
                    .addAsResource(new StringAsset("outside the REST path"), "META-INF/resources/secure"))
            .overrideConfigKey("quarkus.http.root-path", "/root")
            .overrideConfigKey("quarkus.rest.path", "/api")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD /root/api/secure -> " + SecuredResource.class.getName() + "#get"));

    @Test
    void staticResourceIsServedInsteadOfTheShadowedEndpoint() {
        when().get("/api/secure").then().statusCode(200).body(is("static secure"));
        when().get("/secure").then().statusCode(200).body(is("outside the REST path"));
    }

    @Path("/secure")
    public static class SecuredResource {

        @GET
        @RolesAllowed("admin")
        public String get() {
            return "secured endpoint";
        }
    }
}
