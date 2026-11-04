package io.quarkus.resteasy.reactive.server.test.staticresource;

import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithDebugLevelTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FallbackResource.class)
                    .addAsResource(new StringAsset("app"), "META-INF/resources/app/index.html")
                    .addAsResource(new StringAsset("app css"), "META-INF/resources/app/css/app.css")
                    .addAsResource(new StringAsset("favicon"), "META-INF/resources/app/favicon.ico")
                    .addAsResource(new StringAsset("app js"), "META-INF/resources/app/js/app.js")
                    .addAsResource(new StringAsset("vendor js"), "META-INF/resources/app/js/vendor.js")
                    .addAsResource(new StringAsset("manifest"), "META-INF/resources/app/manifest.json"))
            // lists all the paths, not only the first ones
            .overrideRuntimeConfigKey("quarkus.log.category.\"" + LOGGER + "\".level", "DEBUG")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD /app/, /app/css/app.css, /app/favicon.ico, /app/index.html, /app/js/app.js, "
                            + "/app/js/vendor.js, /app/manifest.json -> " + FallbackResource.class.getName() + "#fallback"));

    @AfterAll
    static void resetLogLevel() {
        // the level set with the configuration outlives the application, so it would apply to the next tests
        // TODO: this is arguably a bug and we should investigate and fix it
        Logger.getLogger(LOGGER).setLevel(null);
    }

    @Test
    void staticResourcesAreServedInsteadOfTheShadowedEndpoint() {
        when().get("/app/js/vendor.js").then().statusCode(200).body(is("vendor js"));
        when().get("/app/settings").then().statusCode(200).body(is("fallback endpoint"));
    }

    @Path("/app")
    public static class FallbackResource {

        @GET
        @Path("{path: .*}")
        public String fallback() {
            return "fallback endpoint";
        }
    }
}
