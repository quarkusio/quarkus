package io.quarkus.resteasy.reactive.server.test.staticresource;

import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithIndexPageTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IndexResource.class)
                    .addAsResource(new StringAsset("home"), "META-INF/resources/api/idx/home.html")
                    .addAsResource(new StringAsset("not the index page"), "META-INF/resources/api/other/index.html"))
            .overrideRuntimeConfigKey("quarkus.http.static-resources.index-page", "home.html")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD /api/idx/ -> " + IndexResource.class.getName() + "#index"));

    @Test
    void indexPageIsServedInsteadOfTheShadowedEndpoint() {
        when().get("/api/idx/").then().statusCode(200).body(is("home"));
        when().get("/api/other/").then().statusCode(200).body(is("other endpoint"));
    }

    @Path("/api")
    public static class IndexResource {

        @GET
        @Path("idx")
        public String index() {
            return "index endpoint";
        }

        @GET
        @Path("other")
        public String other() {
            return "other endpoint";
        }
    }
}
