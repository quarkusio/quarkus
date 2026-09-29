package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithServletContextPathTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class)
                    .addAsResource(new StringAsset("static secure"), "META-INF/resources/api/secure")
                    .addAsResource(new StringAsset("index"), "META-INF/resources/api/idx/index.html"))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-undertow", Version.getVersion())))
            .overrideConfigKey("quarkus.http.root-path", "/root")
            .overrideConfigKey("quarkus.servlet.context-path", "/ctx")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD /root/ctx/api/secure -> " + SecuredResource.class.getName() + "#get",
                    "GET, HEAD /root/ctx/api/idx/ -> " + SecuredResource.class.getName() + "#index"));

    @Test
    void staticResourcesAreServedInsteadOfTheShadowedEndpoints() {
        when().get("/ctx/api/secure").then().statusCode(200).body(is("static secure"));
        when().get("/ctx/api/idx/").then().statusCode(200).body(is("index"));
    }

    @Path("/api")
    public static class SecuredResource {

        @GET
        @Path("secure")
        @RolesAllowed("admin")
        public String get() {
            return "secured endpoint";
        }

        @GET
        @Path("idx")
        @RolesAllowed("admin")
        public String index() {
            return "secured index";
        }
    }
}
