package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertNoShadowedEndpoints;
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

class NoShadowedEndpointsWarningWithErrorLevelTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FallbackResource.class)
                    .addAsResource(new StringAsset("app"), "META-INF/resources/app/index.html")
                    .addAsResource(new StringAsset("app js"), "META-INF/resources/app/js/app.js"))
            // disables the warning, e.g. when the shadowing is intended like here
            .overrideRuntimeConfigKey("quarkus.log.category.\"" + LOGGER + "\".level", "ERROR")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertNoShadowedEndpoints(records));

    @AfterAll
    static void resetLogLevel() {
        // the level set with the configuration outlives the application, so it would apply to the next tests
        // TODO: this is arguably a bug and we should investigate and fix it
        Logger.getLogger(LOGGER).setLevel(null);
    }

    @Test
    void staticResourcesAndFallbackEndpointAreServed() {
        when().get("/app/js/app.js").then().statusCode(200).body(is("app js"));
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
