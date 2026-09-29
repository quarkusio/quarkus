package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertNoShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

class NoShadowedEndpointsWarningWithServletRestPathTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class)
                    .addAsResource(new StringAsset("static hello"), "META-INF/resources/api/hello")
                    .addAsResource(new StringAsset("index"), "META-INF/resources/api/idx/index.html"))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-undertow", Version.getVersion())))
            .overrideConfigKey("quarkus.resteasy.path", "/api")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertNoShadowedEndpoints(records));

    @Test
    void endpointsAreServedInsteadOfTheStaticResources() {
        when().get("/api/hello").then().statusCode(200).body(is("hello endpoint"));
        when().get("/api/idx/").then().statusCode(200).body(is("index endpoint"));
    }

    @Path("/")
    public static class HelloResource {

        @GET
        @Path("hello")
        public String hello() {
            return "hello endpoint";
        }

        @GET
        @Path("idx")
        public String index() {
            return "index endpoint";
        }
    }
}
